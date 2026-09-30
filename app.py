import os
from datetime import datetime
import gradio as gr
import librosa
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import torch.nn.functional as F

WEIGHTS_PATH = "best_cat_model.pth"
FEEDBACK_FILE = "user_feedback_log.csv"

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

class CatBehaviorPredictor:
    def __init__(self, weights_path=WEIGHTS_PATH):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.labels = {0: 'Brushing / Attention', 1: 'Waiting for Food', 2: 'Isolation / Distress'}
        self.sr = 8000
        self.target_len = int(self.sr * 2.0)
        
        self.model = CatMeowCNN().to(self.device)
        if os.path.exists(weights_path):
            self.model.load_state_dict(torch.load(weights_path, map_location=self.device, weights_only=True))
            print(f"Loaded trained weights from {weights_path}")
        else:
            print(f"Warning: {weights_path} not found. Running with untrained baseline.")
        self.model.eval()

    def _preprocess_audio(self, audio_path):
        audio, _ = librosa.load(audio_path, sr=self.sr)
        if len(audio) < self.target_len:
            audio = np.pad(audio, (0, self.target_len - len(audio)), mode='constant')
        else:
            audio = audio[:self.target_len]
            
        mel = librosa.feature.melspectrogram(y=audio, sr=self.sr, n_mels=128, hop_length=512)
        mel_db = librosa.power_to_db(mel, ref=np.max)
        mel_norm = (mel_db - mel_db.min()) / (mel_db.max() - mel_db.min() + 1e-6)
        
        tensor = torch.tensor(mel_norm, dtype=torch.float32).unsqueeze(0).unsqueeze(0)
        return tensor.to(self.device)

    def predict(self, audio_path):
        input_tensor = self._preprocess_audio(audio_path)
        with torch.no_grad():
            logits = self.model(input_tensor)
            probs = F.softmax(logits, dim=1).cpu().numpy()[0]
            
        top_idx = int(np.argmax(probs))
        return {
            "predicted_intent": self.labels[top_idx],
            "confidence": float(probs[top_idx]),
            "probabilities": {
                self.labels[i]: round(float(probs[i]), 4) for i in range(len(self.labels))
            }
        }

predictor = CatBehaviorPredictor()

def classify_cat_audio(audio_path):
    if audio_path is None:
        return "لطفاً ابتدا یک فایل صوتی آپلود یا ضبط کنید.", {}
    
    result = predictor.predict(audio_path)
    summary_text = f"رفتار محتمل: {result['predicted_intent']} (اطمینان: {result['confidence']*100:.1f}%)"
    return summary_text, result["probabilities"]

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

with gr.Blocks(title="Cat Vocalization Intent AI") as demo:
    gr.Markdown("# 🐱 سیستم تحلیل و طبقه‌بندی قصد و صدای گربه")
    gr.Markdown("تحلیل رفتار گربه بر اساس الگوهای زمانی-فرکانسی (Mel Spectrogram)")
    
    with gr.Row():
        with gr.Column():
            audio_input = gr.Audio(sources=["upload", "microphone"], type="filepath", label="ورودی صدا")
            predict_btn = gr.Button("تحلیل صدا", variant="primary")
            
        with gr.Column():
            prediction_output = gr.Textbox(label="نتیجه پیش‌بینی مدل", interactive=False)
            confidence_output = gr.Label(num_top_classes=3, label="توزیع احتمالات")
            
    predict_btn.click(
        fn=classify_cat_audio,
        inputs=audio_input,
        outputs=[prediction_output, confidence_output]
    )
    
    gr.Markdown("---")
    gr.Markdown("### بازخورد کاربر (Personalization Loop)")
    with gr.Row():
        verdict_radio = gr.Radio(choices=["Correct", "Wrong"], label="آیا تشخیص درست بود؟", value="Correct")
        correct_dropdown = gr.Dropdown(
            choices=["Brushing / Attention", "Waiting for Food", "Isolation / Distress"],
            label="اگر اشتباه بود، رفتار واقعی چه بود؟",
            value="Waiting for Food"
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