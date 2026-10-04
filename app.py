import os
from datetime import datetime
import gradio as gr
import librosa
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import torch.nn.functional as F
import joblib

from features import extract_audio_features, TARGET_SR

PIPELINE_PATH = "best_cat_pipeline.joblib"
CNN_WEIGHTS_PATH = "best_cat_model.pth"
FEEDBACK_FILE = "user_feedback_log.csv"

# --- نگاشت برچسب‌ها به توضیحات قابل فهم انسانی ---
LABEL_TITLES = {
    0: 'Brushing / Attention (نوازش و شانه زدن)',
    1: 'Waiting for Food (انتظار غذا)',
    2: 'Isolation / Distress (انزوا و احساس تنهایی)'
}
INV_LABEL_MAPPING = {'B': 0, 'F': 1, 'I': 2}

# --- معماری مدل CNN جهت سازگاری و مقایسه ---
class CatMeowCNN(nn.Module):
    def __init__(self, num_classes=3):
        super(CatMeowCNN, self).__init__()
        self.conv1 = nn.Conv2d(1, 16, kernel_size=3, padding=1)
        self.bn1 = nn.BatchNorm2d(16)
        self.pool1 = nn.MaxPool2d(2, 2)
        
        self.conv2 = nn.Conv2d(16, 32, kernel_size=3, padding=1)
        self.bn2 = nn.BatchNorm2d(32)
        self.pool2 = nn.MaxPool2d(2, 2)
        
        self.dropout = nn.Dropout(0.4)
        self.fc = nn.Linear(32 * 32 * 8, num_classes)

    def forward(self, x):
        x = self.pool1(F.relu(self.bn1(self.conv1(x))))
        x = self.pool2(F.relu(self.bn2(self.conv2(x))))
        x = x.view(x.size(0), -1)
        x = self.dropout(x)
        return self.fc(x)

class UnifiedCatPredictor:
    def __init__(self, pipeline_path=PIPELINE_PATH, cnn_path=CNN_WEIGHTS_PATH):
        self.sr = TARGET_SR
        self.target_len = int(self.sr * 2.0)
        self.pipeline_data = None
        self.cnn_model = None
        self.device = torch.device("cpu")

        # 1. بارگذاری مدل بهینه آکوستیک
        if os.path.exists(pipeline_path):
            self.pipeline_data = joblib.load(pipeline_path)
            print(f"Loaded optimized acoustic pipeline from {pipeline_path}")
        else:
            print(f"Notice: {pipeline_path} not found.")

        # 2. بارگذاری مدل CNN پایه
        if os.path.exists(cnn_path):
            self.cnn_model = CatMeowCNN().to(self.device)
            self.cnn_model.load_state_dict(torch.load(cnn_path, map_location=self.device, weights_only=True))
            self.cnn_model.eval()
            print(f"Loaded CNN weights from {cnn_path}")

    def predict_acoustic(self, audio_path):
        if self.pipeline_data is None:
            raise RuntimeError("مدل بهینه آکوستیک یافت نشد.")

        audio, _ = librosa.load(audio_path, sr=self.sr)
        feat_dict = extract_audio_features(audio, sr=self.sr)
        feat_names = self.pipeline_data.get('feature_names')
        if feat_names is not None:
            feat_vec = np.array([feat_dict[k] for k in feat_names]).reshape(1, -1)
        else:
            feat_vec = np.array(list(feat_dict.values())).reshape(1, -1)
        feat_vec = np.nan_to_num(feat_vec, nan=0.0)

        pipeline = self.pipeline_data['pipeline']
        probs = pipeline.predict_proba(feat_vec)[0]
        top_idx = int(np.argmax(probs))

        return {
            "model_type": "Optimized Acoustic Pipeline (دقت 71.4%)",
            "predicted_intent": LABEL_TITLES[top_idx],
            "confidence": float(probs[top_idx]),
            "probabilities": {
                LABEL_TITLES[i]: round(float(probs[i]), 4) for i in range(len(LABEL_TITLES))
            }
        }

    def predict_cnn(self, audio_path):
        if self.cnn_model is None:
            raise RuntimeError("مدل CNN یافت نشد.")

        audio, _ = librosa.load(audio_path, sr=self.sr)
        if len(audio) < self.target_len:
            audio = np.pad(audio, (0, self.target_len - len(audio)), mode='constant')
        else:
            audio = audio[:self.target_len]

        mel = librosa.feature.melspectrogram(y=audio, sr=self.sr, n_mels=128, hop_length=512)
        mel_db = librosa.power_to_db(mel, ref=np.max)
        mel_norm = (mel_db - mel_db.min()) / (mel_db.max() - mel_db.min() + 1e-6)

        tensor = torch.tensor(mel_norm, dtype=torch.float32).unsqueeze(0).unsqueeze(0).to(self.device)
        with torch.no_grad():
            logits = self.cnn_model(tensor)
            probs = F.softmax(logits, dim=1).cpu().numpy()[0]

        top_idx = int(np.argmax(probs))
        return {
            "model_type": "Baseline CNN (دقت 54.0%)",
            "predicted_intent": LABEL_TITLES[top_idx],
            "confidence": float(probs[top_idx]),
            "probabilities": {
                LABEL_TITLES[i]: round(float(probs[i]), 4) for i in range(len(LABEL_TITLES))
            }
        }

    def predict(self, audio_path, model_choice="مدل بهینه جدید (Acoustic Pipeline - دقت 71.4%)"):
        if "CNN" in model_choice:
            return self.predict_cnn(audio_path)
        else:
            return self.predict_acoustic(audio_path)

predictor = UnifiedCatPredictor()

def classify_cat_audio(audio_path, model_choice):
    if audio_path is None:
        return "لطفاً ابتدا یک فایل صوتی آپلود یا ضبط کنید.", {}

    try:
        result = predictor.predict(audio_path, model_choice)
        summary_text = (
            f"🎯 مدل استفاده شده: {result['model_type']}\n"
            f"🐾 قصد و رفتار پیش‌بینی شده: {result['predicted_intent']}\n"
            f"📊 درصد اطمینان مدل: {result['confidence'] * 100:.1f}%"
        )
        return summary_text, result["probabilities"]
    except Exception as e:
        return f"خطا در پردازش صدا: {str(e)}", {}

def save_feedback(audio_path, predicted_text, user_verdict, correct_intent):
    if audio_path is None:
        return "صدایی برای ثبت فیدبک وجود ندارد."

    feedback_entry = {
        "timestamp": datetime.now().isoformat(),
        "audio_path": audio_path,
        "prediction_summary": predicted_text,
        "user_verdict": user_verdict,
        "user_corrected_intent": correct_intent if user_verdict == "Wrong" else "N/A"
    }

    df_fb = pd.DataFrame([feedback_entry])
    if not os.path.exists(FEEDBACK_FILE):
        df_fb.to_csv(FEEDBACK_FILE, index=False)
    else:
        df_fb.to_csv(FEEDBACK_FILE, mode='a', header=False, index=False)

    return f"فیدبک با موفقیت در سیستم ثبت شد ({user_verdict})."

# --- ساخت رابط کاربری Gradio ---
with gr.Blocks(title="Cat Vocalization Intent AI") as demo:
    gr.Markdown("# 🐱 سیستم هوشمند تحلیل و ترجمه صدای گربه (Cat Vocalization Translator)")
    gr.Markdown(
        "این سیستم رفتار و قصد گربه را بر اساس پردازش تخصصی فرکانس پایه (Pitch/F0)، فرمانت‌ها، و انرژی آکوستیک تشخیص می‌دهد."
    )

    with gr.Row():
        with gr.Column():
            audio_input = gr.Audio(sources=["upload", "microphone"], type="filepath", label="ورودی صدای میو گربه")
            model_selector = gr.Radio(
                choices=[
                    "مدل بهینه جدید (Acoustic Pipeline - دقت 71.4%)",
                    "مدل قبلی پایه (CNN - دقت 54.0%)"
                ],
                value="مدل بهینه جدید (Acoustic Pipeline - دقت 71.4%)",
                label="انتخاب مدل هوش مصنوعی"
            )
            predict_btn = gr.Button("🔍 تحلیل و ترجمه صدای گربه", variant="primary")

        with gr.Column():
            prediction_output = gr.Textbox(label="نتیجه تحلیل مدل", interactive=False, lines=4)
            confidence_output = gr.Label(num_top_classes=3, label="توزیع احتمالات رفتارها")

    predict_btn.click(
        fn=classify_cat_audio,
        inputs=[audio_input, model_selector],
        outputs=[prediction_output, confidence_output]
    )

    gr.Markdown("---")
    gr.Markdown("### 📝 بازخورد کاربر و ثبت یادگیری (Active Learning Loop)")
    with gr.Row():
        verdict_radio = gr.Radio(choices=["Correct", "Wrong"], label="آیا تشخیص درست بود؟", value="Correct")
        correct_dropdown = gr.Dropdown(
            choices=[
                "Brushing / Attention (نوازش و شانه زدن)",
                "Waiting for Food (انتظار غذا)",
                "Isolation / Distress (انزوا و احساس تنهایی)"
            ],
            label="اگر اشتباه بود، رفتار واقعی چه بود؟",
            value="Waiting for Food (انتظار غذا)"
        )
        submit_fb_btn = gr.Button("ثبت بازخورد")

    fb_status = gr.Textbox(label="وضعیت لاگ فیدبک", interactive=False)

    submit_fb_btn.click(
        fn=save_feedback,
        inputs=[audio_input, prediction_output, verdict_radio, correct_dropdown],
        outputs=fb_status
    )

if __name__ == "__main__":
    demo.launch()