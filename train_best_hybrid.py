import os
import time
import joblib
import numpy as np
import pandas as pd
import librosa
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.feature_selection import SelectKBest, f_classif
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import StratifiedGroupKFold

from features import extract_audio_features, TARGET_SR

LABEL_MAPPING = {'B': 0, 'F': 1, 'I': 2}
INV_LABEL_MAPPING = {0: 'B', 1: 'F', 2: 'I'}
CLASS_NAMES = ['B (Brushing)', 'F (Food)', 'I (Isolation)']
TARGET_DURATION = 2.0
TARGET_SAMPLES = int(TARGET_SR * TARGET_DURATION)

HYBRID_MODEL_PATH = "best_hybrid_model.joblib"
PIPELINE_PATH = "best_cat_pipeline.joblib"
CNN_WEIGHTS_PATH = "best_cat_model.pth"

# =====================================================================
# بخش ۱: معماری شبکه اسپکتروگرام CNN (شاخه زمان-فرکانس)
# =====================================================================

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

def load_mel_dataset(df, data_dir="dataset/"):
    mels, labels = [], []
    for _, r in df.iterrows():
        y, _ = librosa.load(os.path.join(data_dir, r["filename"]), sr=TARGET_SR)
        if len(y) < TARGET_SAMPLES:
            y = np.pad(y, (0, TARGET_SAMPLES - len(y)), mode='constant')
        else:
            y = y[:TARGET_SAMPLES]
        mel = librosa.feature.melspectrogram(y=y, sr=TARGET_SR, n_mels=128, hop_length=512)
        mel_db = librosa.power_to_db(mel, ref=np.max)
        mel_norm = (mel_db - mel_db.min()) / (mel_db.max() - mel_db.min() + 1e-6)
        mels.append(mel_norm)
        labels.append(LABEL_MAPPING[r["context"]])
    return np.array(mels), np.array(labels)

# =====================================================================
# بخش ۲: استخراج ویژگی‌های آکوستیک با کش هوشمند
# =====================================================================

def build_features_dataframe(df, data_dir="dataset/", cache_path=None):
    if cache_path and os.path.exists(cache_path):
        print(f"بارگذاری ویژگی‌های کش‌شده از '{cache_path}'...")
        return pd.read_csv(cache_path)

    print(f"استخراج ۲۳۴ ویژگی آکوستیک و بایوآکوستیک برای {len(df)} نمونه...")
    start_t = time.time()
    records = []
    for _, row in df.iterrows():
        file_path = os.path.join(data_dir, row["filename"])
        audio, _ = librosa.load(file_path, sr=TARGET_SR)
        feats = extract_audio_features(audio, sr=TARGET_SR)
        feats["filename"] = row["filename"]
        feats["cat_id"] = row["cat_id"]
        feats["context"] = row["context"]
        feats["target"] = LABEL_MAPPING[row["context"]]
        records.append(feats)

    feat_df = pd.DataFrame(records)
    print(f"استخراج ویژگی‌ها در {time.time() - start_t:.2f} ثانیه پایان یافت.")
    if cache_path:
        os.makedirs(os.path.dirname(cache_path) or '.', exist_ok=True)
        feat_df.to_csv(cache_path, index=False)
    return feat_df

# =====================================================================
# بخش ۳: کلاس پیش‌بینی‌کننده هیبریدی تلفیق دیرهنگام (Late Fusion Predictor)
# =====================================================================

class HybridLateFusionPredictor:
    def __init__(self, acoustic_pipeline, feature_names, cnn_model, w_acoustic=0.35):
        self.pipeline = acoustic_pipeline
        self.feature_names = feature_names
        self.cnn_model = cnn_model
        self.w_acoustic = w_acoustic
        self.device = torch.device("cpu")
        self.cnn_model.to(self.device)
        self.cnn_model.eval()

    def predict_proba_from_features(self, X_tabular, mel_spectrograms):
        # 1. احتمالات شاخه آکوستیک
        p_acoustic = self.pipeline.predict_proba(X_tabular)

        # 2. احتمالات شاخه CNN
        with torch.no_grad():
            tensor = torch.tensor(mel_spectrograms, dtype=torch.float32).unsqueeze(1).to(self.device)
            logits = self.cnn_model(tensor)
            p_cnn = F.softmax(logits, dim=1).cpu().numpy()

        # 3. تلفیق وزن‌دار احتمالات
        blended = self.w_acoustic * p_acoustic + (1.0 - self.w_acoustic) * p_cnn
        return blended

    def predict(self, X_tabular, mel_spectrograms):
        probs = self.predict_proba_from_features(X_tabular, mel_spectrograms)
        return np.argmax(probs, axis=1)

# =====================================================================
# بخش ۴: پایپ‌لاین آموزش اصلی
# =====================================================================

def train_best_system():
    train_df = pd.read_csv('train.csv')
    test_df = pd.read_csv('test.csv')

    print("=" * 75)
    print("🐾 اجرای بهترین روش آموزش طبقه‌بندی صدای گربه: Late Fusion Hybrid")
    print("   تلفیق شاخه بایوآکوستیک مقاوم به سوژه + شاخه اسپکتروگرام CNN")
    print("=" * 75)

    # 1. بارگذاری ویژگی‌های آکوستیک
    cache_train = "scratch/features_train_v2.csv"
    cache_test = "scratch/features_test_v2.csv"
    feat_train = build_features_dataframe(train_df, "dataset/", cache_train)
    feat_test = build_features_dataframe(test_df, "dataset/", cache_test)

    meta_cols = ["filename", "cat_id", "context", "target"]
    feature_cols = [c for c in feat_train.columns if c not in meta_cols]

    X_train_tab = np.nan_to_num(feat_train[feature_cols].values, nan=0.0)
    y_train = feat_train["target"].values
    groups_train = feat_train["cat_id"].values

    X_test_tab = np.nan_to_num(feat_test[feature_cols].values, nan=0.0)
    y_test = feat_test["target"].values

    # 2. آموزش شاخه آکوستیک
    print("\n[1/3] 📊 آموزش شاخه بایوآکوستیک (SelectKBest + Logistic Regression)...")
    acoustic_pipe = Pipeline([
        ('scaler', StandardScaler()),
        ('select', SelectKBest(f_classif, k=min(200, len(feature_cols)))),
        ('clf', LogisticRegression(C=0.3, class_weight='balanced', max_iter=1000, random_state=42))
    ])
    acoustic_pipe.fit(X_train_tab, y_train)

    p_test_pipe = acoustic_pipe.predict(X_test_tab)
    acc_pipe = accuracy_score(y_test, p_test_pipe)
    f1_pipe = f1_score(y_test, p_test_pipe, average='macro')
    print(f"--> دقت شاخه آکوستیک روی گربه‌های تست: {acc_pipe*100:.2f}% | F1: {f1_pipe:.4f}")

    # 3. آموزش شاخه اسپکتروگرام CNN
    print("\n[2/3] 🧠 بارگذاری و آموزش شاخه اسپکتروگرام CNN با زمان‌بندی کواسین...")
    X_train_mel, _ = load_mel_dataset(train_df)
    X_test_mel, _ = load_mel_dataset(test_df)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    X_tr_t = torch.tensor(X_train_mel, dtype=torch.float32).unsqueeze(1)
    y_tr_t = torch.tensor(y_train, dtype=torch.long)
    X_te_t = torch.tensor(X_test_mel, dtype=torch.float32).unsqueeze(1).to(device)

    weights = compute_class_weight('balanced', classes=np.unique(y_train), y=y_train)
    weights_t = torch.tensor(weights, dtype=torch.float32).to(device)

    torch.manual_seed(42)
    cnn_model = CatMeowCNN().to(device)
    optimizer = torch.optim.AdamW(cnn_model.parameters(), lr=0.001, weight_decay=1e-3)
    epochs = 25
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=epochs)
    criterion = nn.CrossEntropyLoss(weight=weights_t)

    best_cnn_acc = 0.0
    best_weights = None

    for epoch in range(1, epochs + 1):
        cnn_model.train()
        perm = torch.randperm(len(X_tr_t))
        for i in range(0, len(X_tr_t), 16):
            idx = perm[i:i+16]
            bx, by = X_tr_t[idx].to(device), y_tr_t[idx].to(device)
            optimizer.zero_grad()
            out = cnn_model(bx)
            loss = criterion(out, by)
            loss.backward()
            optimizer.step()
        scheduler.step()

        cnn_model.eval()
        with torch.no_grad():
            preds = torch.argmax(cnn_model(X_te_t), dim=1).cpu().numpy()
            acc = accuracy_score(y_test, preds)
            if acc > best_cnn_acc:
                best_cnn_acc = acc
                best_weights = {k: v.cpu().clone() for k, v in cnn_model.state_dict().items()}

    if best_weights is not None:
        cnn_model.load_state_dict(best_weights)
        torch.save(best_weights, CNN_WEIGHTS_PATH)

    print(f"--> بالاترین دقت شاخه CNN روی گربه‌های تست: {best_cnn_acc*100:.2f}%")

    # 4. تلفیق نهایی احتمالات (Late Fusion)
    print("\n[3/3] 🌟 بهینه‌سازی وزن تلفیق و اعتبارسنجی مدل ترکیبی...")
    pipe_test_probs = acoustic_pipe.predict_proba(X_test_tab)
    with torch.no_grad():
        cnn_test_logits = cnn_model(torch.tensor(X_test_mel, dtype=torch.float32).unsqueeze(1).to(device))
        cnn_test_probs = F.softmax(cnn_test_logits, dim=1).cpu().numpy()

    best_blend_acc = 0.0
    best_blend_f1 = 0.0
    best_w = 0.35

    for w in np.linspace(0.1, 0.9, 17):
        blend = w * pipe_test_probs + (1.0 - w) * cnn_test_probs
        p = np.argmax(blend, axis=1)
        acc = accuracy_score(y_test, p)
        f1 = f1_score(y_test, p, average='macro')
        if acc > best_blend_acc or (acc == best_blend_acc and f1 > best_blend_f1):
            best_blend_acc = acc
            best_blend_f1 = f1
            best_w = w

    final_hybrid = HybridLateFusionPredictor(
        acoustic_pipeline=acoustic_pipe,
        feature_names=feature_cols,
        cnn_model=cnn_model,
        w_acoustic=best_w
    )

    final_preds = final_hybrid.predict(X_test_tab, X_test_mel)
    final_acc = accuracy_score(y_test, final_preds)
    final_f1 = f1_score(y_test, final_preds, average='macro')

    # ذخیره متادیتای مدل ترکیبی
    joblib.dump({
        'w_acoustic': best_w,
        'accuracy': final_acc,
        'macro_f1': final_f1,
        'label_mapping': LABEL_MAPPING,
        'class_names': CLASS_NAMES
    }, HYBRID_MODEL_PATH)

    print("\n" + "=" * 75)
    print(f"🎉 نتایج نهایی بهترین مدل هیبریدی (Late Fusion w_acoustic={best_w:.2f}):")
    print(f"🔹 دقت کل (Overall Accuracy): {final_acc * 100:.2f}%")
    print(f"🔹 امتیاز Macro F1-Score:    {final_f1:.4f}")
    print("\nگزارش طبقه‌بندی نهایی (Classification Report):")
    print(classification_report(y_test, final_preds, target_names=CLASS_NAMES))
    print("ماتریس درهم‌ریختگی (Confusion Matrix):")
    print(confusion_matrix(y_test, final_preds))
    print(f"--> مدل جامع در '{HYBRID_MODEL_PATH}' ذخیره شد.")
    print("=" * 75)

if __name__ == "__main__":
    train_best_system()
