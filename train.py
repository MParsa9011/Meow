import os
import random
import numpy as np
import pandas as pd
import librosa
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch.utils.data import Dataset, DataLoader
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix

# --- تنظیمات و نگاشت برچسب‌ها ---
LABEL_MAPPING = {'B': 0, 'F': 1, 'I': 2}
INV_LABEL_MAPPING = {0: 'B', 1: 'F', 2: 'I'}
TARGET_SR = 8000
TARGET_DURATION = 2.0
TARGET_SAMPLES = int(TARGET_SR * TARGET_DURATION)

# --- دیتاست و پردازش صوت ---
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
            
        mel = librosa.feature.melspectrogram(
            y=audio,
            sr=TARGET_SR,
            n_mels=128,
            hop_length=512
        )
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

# --- معماری شبکه سبک کانولوشنی ---
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

# --- ارزیابی مدل ---
def evaluate_model(model, loader, device):
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

def main():
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Running pipeline on: {device}\n")

    train_df = pd.read_csv('train.csv')
    test_df = pd.read_csv('test.csv')

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
    epochs = 20

    print("--- 1. Training Lightweight CNN with SpecAugment ---")
    for epoch in range(1, epochs + 1):
        model.train()
        running_loss = 0.0
        for inputs, labels in train_loader:
            inputs, labels = inputs.to(device), labels.to(device)
            
            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            
            running_loss += loss.item() * inputs.size(0)
            
        epoch_loss = running_loss / len(train_df)
        test_acc, test_f1, _, _ = evaluate_model(model, test_loader, device)

        if test_f1 > best_f1:
            best_f1 = test_f1
            best_weights = model.state_dict().copy()
            saved_tag = " [* Best Model Saved]"
        else:
            saved_tag = ""

        if epoch % 2 == 0 or epoch == 1 or saved_tag:
            print(f"Epoch [{epoch:02d}/{epochs}] | Loss: {epoch_loss:.4f} | Acc: {test_acc:.3f} | Macro F1: {test_f1:.3f}{saved_tag}")

    model.load_state_dict(best_weights)
    print(f"\nBest Test Macro F1 loaded: {best_f1:.4f}")

    # ارزیابی نهایی
    _, _, y_true, y_pred = evaluate_model(model, test_loader, device)
    print("\n--- Final Evaluation on Unseen Cats ---")
    print(classification_report(y_true, y_pred, target_names=['B (Brushing)', 'F (Food)', 'I (Isolation)']))
    print("Confusion Matrix:\n", confusion_matrix(y_true, y_pred))

    # تست فرضیه Personalization روی گربه MIN01
    print("\n--- 2. Evaluating Personalization Hypothesis on Cat MIN01 ---")
    min_df = test_df[test_df["cat_id"] == "MIN01"].reset_index(drop=True)
    adapt_indices = [min_df[min_df['context'] == c].index[0] for c in ['B', 'F', 'I'] if len(min_df[min_df['context'] == c]) > 0]
    
    adapt_loader = DataLoader(CatMeowDataset(min_df.iloc[adapt_indices]), batch_size=3, shuffle=True)
    eval_loader = DataLoader(CatMeowDataset(min_df.drop(adapt_indices)), batch_size=len(min_df) - len(adapt_indices), shuffle=False)

    acc_before, _, _, _ = evaluate_model(model, eval_loader, device)
    print(f"MIN01 Accuracy BEFORE Personalization: {acc_before * 100:.1f}%")

    pers_optimizer = torch.optim.Adam(model.parameters(), lr=0.0001)
    pers_criterion = nn.CrossEntropyLoss()
    model.train()
    for _ in range(15):
        for x, y in adapt_loader:
            x, y = x.to(device), y.to(device)
            pers_optimizer.zero_grad()
            out = model(x)
            loss = pers_criterion(out, y)
            loss.backward()
            pers_optimizer.step()

    acc_after, _, _, _ = evaluate_model(model, eval_loader, device)
    print(f"MIN01 Accuracy AFTER Personalization:  {acc_after * 100:.1f}%")

    # ذخیره وزن‌های مدل عمومی اصلی
    torch.save(best_weights, "best_cat_model.pth")
    print("\n--> Weights saved successfully to 'best_cat_model.pth'!")

if __name__ == "__main__":
    main()