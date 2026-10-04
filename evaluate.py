import os
import time
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import torch.nn.functional as F
import librosa
import joblib
from sklearn.metrics import accuracy_score, f1_score, precision_recall_fscore_support, confusion_matrix

from features import extract_audio_features, TARGET_SR

LABEL_MAPPING = {'B': 0, 'F': 1, 'I': 2}
CLASS_NAMES = ['B (Brushing)', 'F (Food)', 'I (Isolation)']
TARGET_SAMPLES = int(TARGET_SR * 2.0)

# --- معماری CNN قبلی جهت ارزیابی عادلانه و مقایسه ---
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

def evaluate_cnn(model_path="best_cat_model.pth", test_csv="test.csv", data_dir="dataset/"):
    device = torch.device("cpu")
    model = CatMeowCNN().to(device)
    if not os.path.exists(model_path):
        return None
    model.load_state_dict(torch.load(model_path, map_location=device, weights_only=True))
    model.eval()

    df = pd.read_csv(test_csv)
    targets = [LABEL_MAPPING[c] for c in df["context"]]
    preds = []
    
    start_time = time.time()
    with torch.no_grad():
        for filename in df["filename"]:
            path = os.path.join(data_dir, filename)
            audio, _ = librosa.load(path, sr=TARGET_SR)
            if len(audio) < TARGET_SAMPLES:
                audio = np.pad(audio, (0, TARGET_SAMPLES - len(audio)), mode='constant')
            else:
                audio = audio[:TARGET_SAMPLES]
            mel = librosa.feature.melspectrogram(y=audio, sr=TARGET_SR, n_mels=128, hop_length=512)
            mel_db = librosa.power_to_db(mel, ref=np.max)
            mel_norm = (mel_db - mel_db.min()) / (mel_db.max() - mel_db.min() + 1e-6)
            tensor = torch.tensor(mel_norm, dtype=torch.float32).unsqueeze(0).unsqueeze(0).to(device)
            out = model(tensor)
            p = torch.argmax(out, dim=1).item()
            preds.append(p)
            
    eval_time = (time.time() - start_time) / len(df)
    return np.array(targets), np.array(preds), eval_time

def evaluate_acoustic_pipeline(pipeline_path="best_cat_pipeline.joblib", test_csv="test.csv", data_dir="dataset/"):
    if not os.path.exists(pipeline_path):
        return None
    pipeline_data = joblib.load(pipeline_path)
    model = pipeline_data['pipeline']
    feature_names = pipeline_data.get('feature_names', None)

    df = pd.read_csv(test_csv)
    targets = [LABEL_MAPPING[c] for c in df["context"]]
    preds = []
    
    start_time = time.time()
    for filename in df["filename"]:
        path = os.path.join(data_dir, filename)
        audio, _ = librosa.load(path, sr=TARGET_SR)
        feat_dict = extract_audio_features(audio, sr=TARGET_SR)
        if feature_names is not None:
            feat_vector = np.array([feat_dict[k] for k in feature_names]).reshape(1, -1)
        else:
            feat_vector = np.array(list(feat_dict.values())).reshape(1, -1)
        feat_vector = np.nan_to_num(feat_vector, nan=0.0)
        p = model.predict(feat_vector)[0]
        preds.append(p)
        
    eval_time = (time.time() - start_time) / len(df)
    return np.array(targets), np.array(preds), eval_time

def print_comparison_table():
    print("=" * 70)
    print("        📊 مقایسه عملکرد مدل قبلی (CNN) با مدل بهینه‌سازی‌شده جدید")
    print("=" * 70)

    cnn_res = evaluate_cnn()
    pipe_res = evaluate_acoustic_pipeline()

    if cnn_res is None or pipe_res is None:
        print("یکی از مدل‌ها یافت نشد. لطفاً ابتدا train.py را اجرا نمایید.")
        return

    y_true, cnn_preds, cnn_lat = cnn_res
    _, pipe_preds, pipe_lat = pipe_res

    cnn_acc = accuracy_score(y_true, cnn_preds)
    pipe_acc = accuracy_score(y_true, pipe_preds)

    cnn_f1_macro = f1_score(y_true, cnn_preds, average='macro')
    pipe_f1_macro = f1_score(y_true, pipe_preds, average='macro')

    cnn_f1_weighted = f1_score(y_true, cnn_preds, average='weighted')
    pipe_f1_weighted = f1_score(y_true, pipe_preds, average='weighted')

    cnn_p, cnn_r, cnn_f, _ = precision_recall_fscore_support(y_true, cnn_preds, average=None)
    pipe_p, pipe_r, pipe_f, _ = precision_recall_fscore_support(y_true, pipe_preds, average=None)

    print(f"\n{'معیار (Metric)':<26} | {'مدل فعلی (CNN)':<18} | {'مدل جدید (Acoustic)':<20} | {'تغییر (Diff)':<10}")
    print("-" * 80)
    print(f"{'Overall Accuracy':<26} | {cnn_acc*100:6.2f}%            | {pipe_acc*100:6.2f}%              | {f'+{(pipe_acc-cnn_acc)*100:.2f}%':<10}")
    print(f"{'Macro F1-Score':<26} | {cnn_f1_macro:6.4f}             | {pipe_f1_macro:6.4f}               | {f'+{pipe_f1_macro-cnn_f1_macro:.4f}':<10}")
    print(f"{'Weighted F1-Score':<26} | {cnn_f1_weighted:6.4f}             | {pipe_f1_weighted:6.4f}               | {f'+{pipe_f1_weighted-cnn_f1_weighted:.4f}':<10}")
    print(f"{'Avg Latency per sample':<26} | {cnn_lat*1000:6.1f} ms           | {pipe_lat*1000:6.1f} ms             | {'-':<10}")

    print("\n" + "-" * 80)
    print("تفکیک کلاس‌ها (Per-Class Breakdown):")
    print("-" * 80)
    for i, c_name in enumerate(CLASS_NAMES):
        print(f"👉 کلاس {c_name}:")
        print(f"   - مدل قبلی (CNN):     Precision={cnn_p[i]:.2f}, Recall={cnn_r[i]:.2f}, F1={cnn_f[i]:.2f}")
        print(f"   - مدل جدید (Acoustic): Precision={pipe_p[i]:.2f}, Recall={pipe_r[i]:.2f}, F1={pipe_f[i]:.2f}")

    print("\n" + "=" * 70)
    print("ماتریس درهم‌ریختگی مدل قبلی (CNN):")
    print(confusion_matrix(y_true, cnn_preds))
    print("\nماتریس درهم‌ریختگی مدل جدید (Acoustic Pipeline):")
    print(confusion_matrix(y_true, pipe_preds))
    print("=" * 70)

if __name__ == "__main__":
    print_comparison_table()
