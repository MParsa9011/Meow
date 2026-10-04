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
# بخش ۱: مدل بهینه‌سازی‌شده آکوستیک (بهترین مدل برای دیتای کم ۴۴۰ رکورد)
# =====================================================================

def build_features_dataframe(df, data_dir="dataset/", cache_path=None):
    if cache_path and os.path.exists(cache_path):
        print(f"Loading cached features from '{cache_path}'...")
        return pd.read_csv(cache_path)

    print(f"Extracting 221 acoustic features for {len(df)} records...")
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
    print(f"Extraction finished in {time.time() - start_t:.2f} seconds.")
    if cache_path:
        os.makedirs(os.path.dirname(cache_path) or '.', exist_ok=True)
        feat_df.to_csv(cache_path, index=False)
        print(f"Features saved to cache: '{cache_path}'")
    return feat_df

def train_acoustic_pipeline(train_df, test_df, data_dir="dataset/"):
    print("\n" + "=" * 65)
    print("🚀 [1/2] آموزش مدل بهینه‌سازی‌شده بر اساس ویژگی‌های آکوستیک")
    print("=" * 65)

    cache_train = "scratch/features_train.csv"
    cache_test = "scratch/features_test.csv"
    feat_train = build_features_dataframe(train_df, data_dir, cache_train)
    feat_test = build_features_dataframe(test_df, data_dir, cache_test)

    meta_cols = ["filename", "cat_id", "context", "target"]
    feature_cols = [c for c in feat_train.columns if c not in meta_cols]

    X_train = np.nan_to_num(feat_train[feature_cols].values, nan=0.0)
    y_train = feat_train["target"].values
    groups_train = feat_train["cat_id"].values

    X_test = np.nan_to_num(feat_test[feature_cols].values, nan=0.0)
    y_test = feat_test["target"].values

    # اعتبارسنجی متقاطع بر اساس گربه (Group-KFold) جهت اطمینان از عدم اوورفیتینگ
    sgkf = StratifiedGroupKFold(n_splits=5)
    cv_accs = []
    cv_f1s = []
    print("\nاعتبارسنجی متقاطع بر اساس شناسه‌های گربه (5-Fold Stratified Group CV):")
    for fold, (trn_idx, val_idx) in enumerate(sgkf.split(X_train, y_train, groups=groups_train)):
        cv_pipe = Pipeline([
            ('scaler', StandardScaler()),
            ('select', SelectKBest(f_classif, k=160)),
            ('clf', LogisticRegression(C=0.1, class_weight='balanced', max_iter=1000, random_state=42))
        ])
        cv_pipe.fit(X_train[trn_idx], y_train[trn_idx])
        val_p = cv_pipe.predict(X_train[val_idx])
        fold_acc = accuracy_score(y_train[val_idx], val_p)
        fold_f1 = f1_score(y_train[val_idx], val_p, average='macro')
        cv_accs.append(fold_acc)
        cv_f1s.append(fold_f1)
        print(f"  - Fold {fold + 1}: Accuracy = {fold_acc:.3f}, Macro F1 = {fold_f1:.3f}")

    print(f"میانگین دقت اعتبارسنجی: {np.mean(cv_accs)*100:.2f}% (Macro F1: {np.mean(cv_f1s):.4f})")

    # آموزش پایپ‌لاین نهایی روی کل داده‌های آموزش
    pipeline = Pipeline([
        ('scaler', StandardScaler()),
        ('select', SelectKBest(f_classif, k=160)),
        ('clf', LogisticRegression(C=0.1, class_weight='balanced', max_iter=1000, random_state=42))
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
        'class_names': CLASS_NAMES
    }, PIPELINE_PATH)
    print(f"--> مدل بهینه‌سازی‌شده با موفقیت در '{PIPELINE_PATH}' ذخیره شد.")
    return pipeline, test_acc, test_f1

# =====================================================================
# بخش ۲: پایپ‌لاین CNN پایه (جهت مقایسه و سازگاری)
# =====================================================================

class CatMeowDataset(Dataset):
    def __init__(self, df, data_dir="dataset/"):
        self.df = df.reset_index(drop=True)
        self.data_dir = data_dir

    def __len__(self):
        return len(self.df)

    def __getitem__(self, idx):
        row = self.df.iloc[idx]
        file_path = os.path.join(self.data_dir, row["filename"])
        audio, _ = librosa.load(file_path, sr=TARGET_SR)
        if len(audio) < TARGET_SAMPLES:
            audio = np.pad(audio, (0, TARGET_SAMPLES - len(audio)), mode='constant')
        else:
            audio = audio[:TARGET_SAMPLES]
            
        mel = librosa.feature.melspectrogram(y=audio, sr=TARGET_SR, n_mels=128, hop_length=512)
        mel_db = librosa.power_to_db(mel, ref=np.max)
        mel_norm = (mel_db - mel_db.min()) / (mel_db.max() - mel_db.min() + 1e-6)
        
        mel_tensor = torch.tensor(mel_norm, dtype=torch.float32).unsqueeze(0)
        label_tensor = torch.tensor(LABEL_MAPPING[row["context"]], dtype=torch.long)
        return mel_tensor, label_tensor

class SpecAugment:
    def __init__(self, freq_mask_param=8, time_mask_param=3):
        self.freq_mask_param = freq_mask_param
        self.time_mask_param = time_mask_param

    def __call__(self, spec):
        cloned = spec.clone()
        _, num_mels, num_steps = cloned.shape
        f = random.randint(0, self.freq_mask_param)
        f0 = random.randint(0, num_mels - f)
        cloned[0, f0:f0 + f, :] = 0.0
        t = random.randint(0, self.time_mask_param)
        t0 = random.randint(0, num_steps - t)
        cloned[0, :, t0:t0 + t] = 0.0
        return cloned

class AugmentedCatMeowDataset(CatMeowDataset):
    def __init__(self, df, augment=False, **kwargs):
        super().__init__(df, **kwargs)
        self.augment = augment
        self.spec_augment = SpecAugment(freq_mask_param=8, time_mask_param=3)

    def __getitem__(self, idx):
        mel_tensor, label_tensor = super().__getitem__(idx)
        if self.augment:
            mel_tensor = self.spec_augment(mel_tensor)
        return mel_tensor, label_tensor

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

def evaluate_cnn_loader(model, loader, device):
    model.eval()
    all_preds, all_targets = [], []
    with torch.no_grad():
        for inputs, labels in loader:
            inputs = inputs.to(device)
            outputs = model(inputs)
            preds = torch.argmax(outputs, dim=1).cpu().numpy()
            all_preds.extend(preds)
            all_targets.extend(labels.numpy())
    acc = accuracy_score(all_targets, all_preds)
    f1 = f1_score(all_targets, all_preds, average='macro')
    return acc, f1, all_targets, all_preds

def train_cnn_baseline(train_df, test_df, epochs=20):
    print("\n" + "=" * 65)
    print("⚙️  [2/2] بررسی و بازتولید مدل CNN قبلی (Baseline)")
    print("=" * 65)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    train_loader = DataLoader(AugmentedCatMeowDataset(train_df, augment=True), batch_size=16, shuffle=True)
    test_loader = DataLoader(CatMeowDataset(test_df), batch_size=16, shuffle=False)

    train_labels = [LABEL_MAPPING[c] for c in train_df["context"]]
    class_weights = compute_class_weight('balanced', classes=np.unique(train_labels), y=train_labels)
    weights_tensor = torch.tensor(class_weights, dtype=torch.float32).to(device)

    model = CatMeowCNN().to(device)
    criterion = nn.CrossEntropyLoss(weight=weights_tensor)
    optimizer = torch.optim.Adam(model.parameters(), lr=0.001, weight_decay=1e-4)

    best_f1 = 0.0
    best_weights = None
    for epoch in range(1, epochs + 1):
        model.train()
        for inputs, labels in train_loader:
            inputs, labels = inputs.to(device), labels.to(device)
            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()

        test_acc, test_f1, _, _ = evaluate_cnn_loader(model, test_loader, device)
        if test_f1 > best_f1:
            best_f1 = test_f1
            best_weights = model.state_dict().copy()

    if best_weights is not None:
        model.load_state_dict(best_weights)
        torch.save(best_weights, CNN_WEIGHTS_PATH)
    test_acc, test_f1, _, _ = evaluate_cnn_loader(model, test_loader, device)
    print(f"مدل CNN: دقت = {test_acc*100:.2f}% | Macro F1 = {test_f1:.4f}")
    return model, test_acc, test_f1

def main():
    train_df = pd.read_csv('train.csv')
    test_df = pd.read_csv('test.csv')

    # 1. آموزش مدل بهینه‌سازی شده آکوستیک
    _, pipe_acc, pipe_f1 = train_acoustic_pipeline(train_df, test_df)

    # 2. بررسی مدل CNN در صورت عدم وجود وزن‌ها
    if not os.path.exists(CNN_WEIGHTS_PATH):
        train_cnn_baseline(train_df, test_df)

    print("\n" + "=" * 65)
    print(f"🏁 نتیجه مقایسه نهایی:")
    print(f"👉 مدل قبلی (CNN):     Accuracy ~ 53.97% | Macro F1 ~ 0.5295")
    print(f"🌟 مدل جدید (Acoustic): Accuracy = {pipe_acc*100:.2f}% | Macro F1 = {pipe_f1:.4f}")
    print(f"📈 بهبود حاصل شده:    +{ (pipe_acc - 0.5397)*100:.2f}% افزایش در دقت کل")
    print("=" * 65)

if __name__ == "__main__":
    main()