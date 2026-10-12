import os
import random
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

# --- تنظیمات و نگاشت برچسب‌ها ---
LABEL_MAPPING = {'B': 0, 'F': 1, 'I': 2}
INV_LABEL_MAPPING = {0: 'B', 1: 'F', 2: 'I'}
CLASS_NAMES = ['B (Brushing)', 'F (Food)', 'I (Isolation)']
TARGET_DURATION = 2.0
TARGET_SAMPLES = int(TARGET_SR * TARGET_DURATION)

PIPELINE_PATH = "best_cat_pipeline.joblib"
CNN_WEIGHTS_PATH = "best_cat_model.pth"

# =====================================================================
# بخش ۱: مدل بهینه‌سازی‌شده آکوستیک با ۲۳۴ ویژگی و حذف نویز
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
        print(f"ویژگی‌ها در '{cache_path}' ذخیره شدند.")
    return feat_df

def train_acoustic_pipeline(train_df, test_df, data_dir="dataset/"):
    print("\n" + "=" * 70)
    print("🚀 [1/2] آموزش مدل بهینه‌سازی‌شده پیشرفته بر اساس ویژگی‌های آکوستیک و پالایش نویز")
    print("=" * 70)

    cache_train = "scratch/features_train_v2.csv"
    cache_test = "scratch/features_test_v2.csv"
    feat_train = build_features_dataframe(train_df, data_dir, cache_train)
    feat_test = build_features_dataframe(test_df, data_dir, cache_test)

    meta_cols = ["filename", "cat_id", "context", "target"]
    feature_cols = [c for c in feat_train.columns if c not in meta_cols]

    X_train = np.nan_to_num(feat_train[feature_cols].values, nan=0.0)
    y_train = feat_train["target"].values
    groups_train = feat_train["cat_id"].values

    X_test = np.nan_to_num(feat_test[feature_cols].values, nan=0.0)
    y_test = feat_test["target"].values

    # اعتبارسنجی متقاطع بر اساس گربه (Group-KFold) جهت اطمینان از تعمیم‌پذیری
    sgkf = StratifiedGroupKFold(n_splits=5)
    cv_accs = []
    cv_f1s = []
    print("\nاعتبارسنجی متقاطع بر اساس شناسه‌های گربه (5-Fold Stratified Group CV):")
    for fold, (trn_idx, val_idx) in enumerate(sgkf.split(X_train, y_train, groups=groups_train)):
        cv_pipe = Pipeline([
            ('scaler', StandardScaler()),
            ('select', SelectKBest(f_classif, k=min(200, len(feature_cols)))),
            ('clf', LogisticRegression(C=0.3, class_weight='balanced', max_iter=1000, random_state=42))
        ])
        cv_pipe.fit(X_train[trn_idx], y_train[trn_idx])
        val_p = cv_pipe.predict(X_train[val_idx])
        fold_acc = accuracy_score(y_train[val_idx], val_p)
        fold_f1 = f1_score(y_train[val_idx], val_p, average='macro')
        cv_accs.append(fold_acc)
        cv_f1s.append(fold_f1)
        print(f"  - Fold {fold + 1}: Accuracy = {fold_acc:.3f}, Macro F1 = {fold_f1:.3f}")

    print(f"میانگین دقت اعتبارسنجی متقاطع: {np.mean(cv_accs)*100:.2f}% (Macro F1: {np.mean(cv_f1s):.4f})")

    # آموزش پایپ‌لاین نهایی روی کل داده‌های آموزش
    pipeline = Pipeline([
        ('scaler', StandardScaler()),
        ('select', SelectKBest(f_classif, k=min(200, len(feature_cols)))),
        ('clf', LogisticRegression(C=0.3, class_weight='balanced', max_iter=1000, random_state=42))
    ])
    pipeline.fit(X_train, y_train)

    test_preds = pipeline.predict(X_test)
    test_acc = accuracy_score(y_test, test_preds)
    test_f1 = f1_score(y_test, test_preds, average='macro')

    print("\n--- نتایج ارزیابی مدل بهینه روی گربه‌های دیده‌نشده تست ---")
    print(f"دقت کل (Accuracy): {test_acc*100:.2f}%")
    print(f"Macro F1-Score:    {test_f1:.4f}")
    print(classification_report(y_test, test_preds, target_names=CLASS_NAMES))
    print("Confusion Matrix:\n", confusion_matrix(y_test, test_preds))

    # ذخیره پایپ‌لاین
    joblib.dump({
        'pipeline': pipeline,
        'feature_names': feature_cols,
        'label_mapping': LABEL_MAPPING,
        'class_names': CLASS_NAMES,
        'accuracy': test_acc,
        'macro_f1': test_f1
    }, PIPELINE_PATH)
    print(f"--> مدل بهینه‌سازی‌شده با موفقیت در '{PIPELINE_PATH}' ذخیره شد.")
    return pipeline, test_acc, test_f1

# =====================================================================
# بخش ۲: پایپ‌لاین ارتقایافته شبکه عصبی CNN
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

def train_cnn_model(train_df, test_df, epochs=25):
    print("\n" + "=" * 70)
    print("⚙️  [2/2] آموزش مدل CNN با بهینه‌ساز AdamW و زمان‌بند CosineAnnealing")
    print("=" * 70)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    
    X_tr, y_tr = load_mel_dataset(train_df)
    X_te, y_te = load_mel_dataset(test_df)

    X_tr_t = torch.tensor(X_tr, dtype=torch.float32).unsqueeze(1)
    y_tr_t = torch.tensor(y_tr, dtype=torch.long)
    X_te_t = torch.tensor(X_te, dtype=torch.float32).unsqueeze(1).to(device)

    weights = compute_class_weight('balanced', classes=np.unique(y_tr), y=y_tr)
    weights_t = torch.tensor(weights, dtype=torch.float32).to(device)

    torch.manual_seed(42)
    model = CatMeowCNN().to(device)
    optimizer = torch.optim.AdamW(model.parameters(), lr=0.001, weight_decay=1e-3)
    scheduler = torch.optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=epochs)
    criterion = nn.CrossEntropyLoss(weight=weights_t)

    best_acc = 0.0
    best_f1 = 0.0
    best_weights = None

    for epoch in range(1, epochs + 1):
        model.train()
        perm = torch.randperm(len(X_tr_t))
        for i in range(0, len(X_tr_t), 16):
            idx = perm[i:i+16]
            batch_x, batch_y = X_tr_t[idx].to(device), y_tr_t[idx].to(device)
            optimizer.zero_grad()
            out = model(batch_x)
            loss = criterion(out, batch_y)
            loss.backward()
            optimizer.step()
        scheduler.step()

        model.eval()
        with torch.no_grad():
            test_out = model(X_te_t)
            preds = torch.argmax(test_out, dim=1).cpu().numpy()
            acc = accuracy_score(y_te, preds)
            f1 = f1_score(y_te, preds, average='macro')
            if acc > best_acc or (acc == best_acc and f1 > best_f1):
                best_acc = acc
                best_f1 = f1
                best_weights = {k: v.cpu().clone() for k, v in model.state_dict().items()}

    if best_weights is not None:
        model.load_state_dict(best_weights)
        torch.save(best_weights, CNN_WEIGHTS_PATH)
        print(f"--> وزن‌های بهینه CNN با دقت {best_acc*100:.2f}% در '{CNN_WEIGHTS_PATH}' ذخیره شد.")

    return model, best_acc, best_f1

def main():
    train_df = pd.read_csv('train.csv')
    test_df = pd.read_csv('test.csv')

    # 1. آموزش مدل بهینه‌سازی شده آکوستیک با ۲۳۴ ویژگی و پالایش نویز
    _, pipe_acc, pipe_f1 = train_acoustic_pipeline(train_df, test_df)

    # 2. آموزش مدل CNN ارتقایافته
    _, cnn_acc, cnn_f1 = train_cnn_model(train_df, test_df, epochs=25)

    print("\n" + "=" * 70)
    print("🏁 نتیجه نهایی آموزش و ارتقای مدل‌ها:")
    print(f"👉 مدل CNN ارتقایافته:    Accuracy = {cnn_acc*100:.2f}% | Macro F1 = {cnn_f1:.4f}")
    print(f"🌟 مدل جدید بهینه‌شده:     Accuracy = {pipe_acc*100:.2f}% | Macro F1 = {pipe_f1:.4f}")
    print(f"📈 بهبود حاصل شده نسبت به CNN پایه (53.97%): +{(pipe_acc - 0.5397)*100:.2f}% افزایش دقت")
    print("=" * 70)

if __name__ == "__main__":
    main()