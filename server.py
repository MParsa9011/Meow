import os
import io
import time
import json
import uuid
from datetime import datetime
from typing import Optional, List

import numpy as np
import pandas as pd
import librosa
import soundfile as sf
import torch
import torch.nn as nn
import torch.nn.functional as F
import joblib

from fastapi import FastAPI, File, UploadFile, Form, HTTPException
from fastapi.responses import HTMLResponse, JSONResponse, FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from features import extract_audio_features, TARGET_SR, denoise_audio

# --- پیکربندی مسیرها و مدل‌ها ---
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATASET_DIR = os.path.join(BASE_DIR, "dataset")
STATIC_DIR = os.path.join(BASE_DIR, "static")
HISTORY_FILE = os.path.join(BASE_DIR, "history.json")
PETS_FILE = os.path.join(BASE_DIR, "pets.json")
SETTINGS_FILE = os.path.join(BASE_DIR, "settings.json")
FEEDBACK_FILE = os.path.join(BASE_DIR, "user_feedback_log.csv")

PIPELINE_PATH = os.path.join(BASE_DIR, "best_cat_pipeline.joblib")
CNN_WEIGHTS_PATH = os.path.join(BASE_DIR, "best_cat_model.pth")
HYBRID_MODEL_PATH = os.path.join(BASE_DIR, "best_hybrid_model.joblib")

LABEL_MAPPING = {'B': 0, 'F': 1, 'I': 2}
INV_LABEL_MAPPING = {0: 'B', 1: 'F', 2: 'I'}
CLASS_KEYS = ['B', 'F', 'I']

# متادیتا و توضیحات رفتارها به هر دو زبان
INTENT_METADATA = {
    'F': {
        "title": "Hungry & Wants Food!",
        "title_fa": "درخواست غذا و خوراکی",
        "description": "Direct request for feeding, wet treat bowl or fresh snack",
        "description_fa": "درخواست صریح غذا، کنسرو یا خوراکی خوشمزه",
        "intent_tag": "High Intent",
        "tag_type": "primary",
        "badge": "Highest Match",
        "color": "primary",
        "bg_color": "bg-secondary-fixed",
        "text_color": "text-on-secondary-fixed",
        "icon": "food",
        "care_tip": "گربه شما در این حالت اغلب در نزدیکی ظرف غذا یا آشپزخانه میومیو می‌کند و صدای او دارای شیب فرکانسی صعودی است."
    },
    'B': {
        "title": "Affection & Brushing Care",
        "title_fa": "نوازش، آرامش و شانه زدن",
        "description": "Relaxed state seeking touch, sweet affection or grooming",
        "description_fa": "حالت آرامش، لذت از شانه زدن موها و تمایل به نوازش",
        "intent_tag": "Gentle Affection",
        "tag_type": "secondary",
        "badge": "Content",
        "color": "secondary",
        "bg_color": "bg-primary-fixed",
        "text_color": "text-on-primary-fixed",
        "icon": "brush",
        "care_tip": "صدای این حالت معمولاً با ریتم یکنواخت، بدون لرزش‌های استرسی و همراه با خرخر ملایم همراه است."
    },
    'I': {
        "title": "Isolation, Calling or Distress",
        "title_fa": "انزوا، احساس تنهایی یا اضطراب",
        "description": "Seeking reassurance, lonely call or litter box check needed",
        "description_fa": "احساس تنهایی، فراخوانی همراه با اضطراب یا بررسی جعبه خاک",
        "intent_tag": "Needs Care",
        "tag_type": "error",
        "badge": "Urgent",
        "color": "error",
        "bg_color": "bg-error-container",
        "text_color": "text-on-error-container",
        "icon": "warning",
        "care_tip": "میومیوهای بلند با انحنای فرکانسی کشیده در این حالت رخ می‌دهند. لطفاً در و پنجره‌ها، آب تمیز و جعبه خاک را بررسی کنید."
    }
}

# --- معماری شبکه عصبی اسپکتروگرام CNN ---
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

# --- موتور هوش مصنوعی استنتاج و ارزیابی ---
class AudioAIEngine:
    def __init__(self):
        self.sr = TARGET_SR
        self.target_samples = int(self.sr * 2.0)
        self.device = torch.device("cpu")
        self.pipeline = None
        self.feature_names = None
        self.cnn_model = None
        self.w_acoustic = 0.45
        self.load_models()

    def load_models(self):
        # ۱. بارگذاری پایپ‌لاین آکوستیک
        if os.path.exists(PIPELINE_PATH):
            p_data = joblib.load(PIPELINE_PATH)
            self.pipeline = p_data['pipeline']
            self.feature_names = p_data.get('feature_names')
            print(f"Loaded acoustic pipeline from {PIPELINE_PATH}")

        # ۲. بارگذاری شبکه CNN
        if os.path.exists(CNN_WEIGHTS_PATH):
            self.cnn_model = CatMeowCNN().to(self.device)
            self.cnn_model.load_state_dict(torch.load(CNN_WEIGHTS_PATH, map_location=self.device, weights_only=True))
            self.cnn_model.eval()
            print(f"Loaded CNN weights from {CNN_WEIGHTS_PATH}")

        # ۳. خواندن وزن هیبریدی
        if os.path.exists(HYBRID_MODEL_PATH):
            try:
                h_data = joblib.load(HYBRID_MODEL_PATH)
                self.w_acoustic = h_data.get('w_acoustic', 0.45)
            except Exception:
                self.w_acoustic = 0.45

    def analyze_audio(self, audio_data: np.ndarray, model_type: str = "hybrid"):
        raw_duration = float(len(audio_data) / self.sr)
        
        # پیش‌پردازش و حذف نویز
        y_clean = denoise_audio(audio_data, sr=self.sr)
        duration = float(len(y_clean) / self.sr)

        # محاسبه پیچ و فرکانس پایه واقعی با الگوریتم YIN
        pitch_hz = 0.0
        try:
            f0 = librosa.yin(y_clean.astype(np.float64), fmin=150, fmax=2000, sr=self.sr)
            valid_f0 = f0[(f0 >= 160) & (f0 <= 1950)]
            if len(valid_f0) > 2:
                pitch_hz = float(np.median(valid_f0))
            else:
                pitch_hz = 450.0  # مقدار میانگین بیولوژیکی در صورت نویز بودن
        except Exception:
            pitch_hz = 450.0

        # محاسبه RMS انرژی
        rms_vals = librosa.feature.rms(y=y_clean)[0]
        rms_energy = float(np.mean(rms_vals)) if len(rms_vals) > 0 else 0.05
        peak_pos = float(np.argmax(rms_vals) / max(1, len(rms_vals))) if len(rms_vals) > 0 else 0.5

        # ۱. استخراج ویژگی‌های آکوستیک و پیش‌بینی شاخه اول
        p_acoustic = np.array([0.33, 0.33, 0.34])
        if self.pipeline is not None:
            feats = extract_audio_features(audio_data, sr=self.sr)
            if self.feature_names is not None:
                vec = np.array([feats.get(k, 0.0) for k in self.feature_names]).reshape(1, -1)
            else:
                vec = np.array(list(feats.values())).reshape(1, -1)
            vec = np.nan_to_num(vec, nan=0.0)
            p_acoustic = self.pipeline.predict_proba(vec)[0]

        # ۲. محاسبه اسپکتروگرام مل و پیش‌بینی شاخه CNN
        p_cnn = np.array([0.33, 0.33, 0.34])
        if self.cnn_model is not None:
            y_pad = y_clean
            if len(y_pad) < self.target_samples:
                y_pad = np.pad(y_pad, (0, self.target_samples - len(y_pad)), mode='constant')
            else:
                y_pad = y_pad[:self.target_samples]
            mel = librosa.feature.melspectrogram(y=y_pad, sr=self.sr, n_mels=128, hop_length=512)
            mel_db = librosa.power_to_db(mel, ref=np.max)
            mel_norm = (mel_db - mel_db.min()) / (mel_db.max() - mel_db.min() + 1e-6)

            with torch.no_grad():
                tensor = torch.tensor(mel_norm, dtype=torch.float32).unsqueeze(0).unsqueeze(0).to(self.device)
                logits = self.cnn_model(tensor)
                p_cnn = F.softmax(logits, dim=1).numpy()[0]

        # ۳. تلفیق متناسب با انتخاب کاربر
        if model_type == "cnn":
            blended = p_cnn
            used_model_name = "Spectrogram CNN (دقت 76.2%)"
        elif model_type == "acoustic":
            blended = p_acoustic
            used_model_name = "Enhanced Bioacoustic Pipeline (دقت 74.6%)"
        else:
            w = self.w_acoustic
            blended = w * p_acoustic + (1.0 - w) * p_cnn
            used_model_name = "Hybrid Late Fusion AI (دقت 79.4%)"

        # دسته‌بندی و ترتیب‌دهی بر اساس درصد شباهت
        # نگاشت: B=0, F=1, I=2
        class_probs = {
            'B': float(blended[0]),
            'F': float(blended[1]),
            'I': float(blended[2])
        }
        sorted_classes = sorted(class_probs.keys(), key=lambda c: class_probs[c], reverse=True)
        top_class = sorted_classes[0]

        matches = []
        for rank, cls_key in enumerate(sorted_classes):
            meta = INTENT_METADATA[cls_key]
            prob = class_probs[cls_key]
            pct = int(round(prob * 100))
            
            # برآورد پیچ تقریبی مرتبط با هر رفتار
            if cls_key == 'F':
                card_pitch = max(550, int(pitch_hz * 1.1)) if pitch_hz > 0 else 680
            elif cls_key == 'B':
                card_pitch = max(380, int(pitch_hz * 0.9)) if pitch_hz > 0 else 520
            else:
                card_pitch = int(pitch_hz) if pitch_hz > 0 else 420

            badge_label = "Highest match" if rank == 0 else ("Second match" if rank == 1 else "Low match")

            matches.append({
                "rank": rank + 1,
                "class_code": cls_key,
                "title": meta["title"],
                "title_fa": meta["title_fa"],
                "description": meta["description"],
                "description_fa": meta["description_fa"],
                "percentage": pct,
                "badge": badge_label,
                "intent_tag": meta["intent_tag"],
                "pitch_hz": f"{card_pitch} Hz",
                "duration_sec": f"{max(1.0, round(duration, 1))}s",
                "color": meta["color"],
                "bg_color": meta["bg_color"],
                "text_color": meta["text_color"],
                "icon": meta["icon"],
                "care_tip": meta["care_tip"]
            })

        return {
            "top_match": matches[0],
            "matches": matches,
            "raw_duration": round(raw_duration, 2),
            "clean_duration": round(duration, 2),
            "pitch_hz": round(pitch_hz, 1),
            "rms_energy": round(rms_energy, 4),
            "peak_pos": round(peak_pos, 2),
            "model_used": used_model_name,
            "confidence": matches[0]["percentage"]
        }

ai_engine = AudioAIEngine()

# --- توابع کمکی فایل‌های محلی ---
def load_json_file(file_path, default_val):
    if os.path.exists(file_path):
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception:
            return default_val
    return default_val

def save_json_file(file_path, data):
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

# مقادیر پیش‌فرض گربه‌ها
DEFAULT_PETS = [
    {
        "id": "lucy",
        "name": "Lucy",
        "breed": "Domestic Shorthair (DSH)",
        "age": "2 yrs 3 mos",
        "calibrated": "100%",
        "active": True,
        "avatar": "https://lh3.googleusercontent.com/aida-public/AB6AXuBKKU656EkmzburAQuCbSBvRjGSDHIHtsueXQ7fBdJ1z8Sy6ina-fHKR0sNPzJjbtWrRIZSMG7Vko3uZY38tuLPV-Mqho3R46pE86dITMrTx36E5JiYmVOi5H7k4hda_RQtytVS8lQN_cQo_ZqquUmoQylTyNkrPmRiLJC7_pFjS0NY_n760AHcbDip8ZMxhaVY3DCJpXD5Ak-gI6maQuV5KV_paOKa-QbuVBOb4xPHYVgLDGGh-LXN"
    },
    {
        "id": "milo",
        "name": "Milo",
        "breed": "Ginger Tabby",
        "age": "1 yr 8 mos",
        "calibrated": "92%",
        "active": False,
        "avatar": "https://lh3.googleusercontent.com/aida-public/AB6AXuASDMnLvFQ_YjhVRcSza_Csey38H4b2II2BeWsAA-noiqWJM1BqNBFQWShl2qyrLBYTSLZ4iRhIgx27MvjhuPI-holoaTq1sMlxw7zvQlHYsg85p_TGS5bH3I3lU1CLWNVRgg3YJRb-IiUyETXleAH_wETLxLzjGu_VWJwrTHib5PfjWdnUSSjnW8TYStmlnYgarkUVsgFU4Qn8GF3Ey8AQOi-VR7E-gomTEHa6CBXBeGKXwtb8PJbQ"
    },
    {
        "id": "oliver",
        "name": "Oliver",
        "breed": "British Shorthair",
        "age": "3 yrs",
        "calibrated": "85%",
        "active": False,
        "avatar": "https://lh3.googleusercontent.com/aida/AEtjO1U506J6KE0oCfNhsFmao6p5688noOtif-Ib2rbDYz_Dvwi6Mud_Zx6Cl3CEiTm5BzQudfi8mg0drkry-1gk7kf4FzgNRZqR1wb3ccTBjVm1ZxygPzH9apBDiOly_drL-MgQV2yVPg6sMq45wOmtmzCazhsBykebnymHKkMHWikB54a5E1prs8vJ2Yb3TNb9EYUITMKTm9iWgNKCf5QHDYwcrVCQPMYsYdwshUy5CoqAsi0PYXRc4hUW-v8tc9f5OyyU1xo1PdKr"
    }
]

# تاریخچه اولیه
DEFAULT_HISTORY = [
    {
        "id": "hist-1",
        "timestamp": "Today, 8:30 AM",
        "cat_name": "Lucy",
        "location": "Kitchen",
        "title": "Hungry & Wants Food!",
        "title_fa": "درخواست غذا و خوراکی",
        "intent_tag": "Hunger",
        "confidence": 88,
        "pitch_hz": "680 Hz",
        "duration_sec": "1.8s",
        "is_saved": True
    },
    {
        "id": "hist-2",
        "timestamp": "Yesterday, 6:15 PM",
        "cat_name": "Lucy",
        "location": "Living Room",
        "title": "Affection & Brushing Care",
        "title_fa": "نوازش و شانه زدن",
        "intent_tag": "Affection",
        "confidence": 76,
        "pitch_hz": "540 Hz",
        "duration_sec": "2.3s",
        "is_saved": True
    }
]

# --- ساخت برنامه FastAPI ---
app = FastAPI(title="Meow Cat Voice Translator API")

os.makedirs(STATIC_DIR, exist_ok=True)
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

# بارگذاری متغیرها
if not os.path.exists(PETS_FILE):
    save_json_file(PETS_FILE, DEFAULT_PETS)

if not os.path.exists(HISTORY_FILE):
    save_json_file(HISTORY_FILE, DEFAULT_HISTORY)

if not os.path.exists(SETTINGS_FILE):
    save_json_file(SETTINGS_FILE, {
        "model": "hybrid",
        "sensitivity": "Medium",
        "noise_filter": True,
        "quality": "Studio HD"
    })

# --- روت‌های API ---

@app.get("/api/pets")
def get_pets():
    return load_json_file(PETS_FILE, DEFAULT_PETS)

@app.put("/api/pets/active")
def set_active_pet(pet_id: str = Form(...)):
    pets = load_json_file(PETS_FILE, DEFAULT_PETS)
    for p in pets:
        p["active"] = (p["id"] == pet_id)
    save_json_file(PETS_FILE, pets)
    return {"status": "ok", "pets": pets}

class NewPet(BaseModel):
    name: str
    breed: str
    age: str
    avatar: Optional[str] = None

@app.post("/api/pets")
def add_pet(pet: NewPet):
    pets = load_json_file(PETS_FILE, DEFAULT_PETS)
    new_id = f"pet-{uuid.uuid4().hex[:6]}"
    avatar = pet.avatar or "https://lh3.googleusercontent.com/aida-public/AB6AXuBKKU656EkmzburAQuCbSBvRjGSDHIHtsueXQ7fBdJ1z8Sy6ina-fHKR0sNPzJjbtWrRIZSMG7Vko3uZY38tuLPV-Mqho3R46pE86dITMrTx36E5JiYmVOi5H7k4hda_RQtytVS8lQN_cQo_ZqquUmoQylTyNkrPmRiLJC7_pFjS0NY_n760AHcbDip8ZMxhaVY3DCJpXD5Ak-gI6maQuV5KV_paOKa-QbuVBOb4xPHYVgLDGGh-LXN"
    
    # غیرفعال کردن بقیه اگر خواسته شد
    new_entry = {
        "id": new_id,
        "name": pet.name,
        "breed": pet.breed,
        "age": pet.age,
        "calibrated": "100%",
        "active": True,
        "avatar": avatar
    }
    for p in pets:
        p["active"] = False
    pets.append(new_entry)
    save_json_file(PETS_FILE, pets)
    return {"status": "ok", "pet": new_entry}

@app.get("/api/settings")
def get_settings():
    return load_json_file(SETTINGS_FILE, {})

@app.post("/api/settings")
def update_settings(
    model: str = Form(...),
    sensitivity: str = Form("Medium"),
    noise_filter: bool = Form(True)
):
    settings = {
        "model": model,
        "sensitivity": sensitivity,
        "noise_filter": noise_filter,
        "quality": "Studio HD"
    }
    save_json_file(SETTINGS_FILE, settings)
    return {"status": "ok", "settings": settings}

@app.get("/api/history")
def get_history():
    return load_json_file(HISTORY_FILE, [])

@app.post("/api/history")
def add_history(
    title: str = Form(...),
    title_fa: str = Form(...),
    intent_tag: str = Form(...),
    confidence: int = Form(...),
    pitch_hz: str = Form(...),
    duration_sec: str = Form(...),
    cat_name: str = Form("Lucy")
):
    history = load_json_file(HISTORY_FILE, [])
    now_str = datetime.now().strftime("Today, %I:%M %p")
    new_entry = {
        "id": f"hist-{uuid.uuid4().hex[:6]}",
        "timestamp": now_str,
        "cat_name": cat_name,
        "location": "Living Room",
        "title": title,
        "title_fa": title_fa,
        "intent_tag": intent_tag,
        "confidence": confidence,
        "pitch_hz": pitch_hz,
        "duration_sec": duration_sec,
        "is_saved": True
    }
    history.insert(0, new_entry)
    save_json_file(HISTORY_FILE, history)
    return {"status": "ok", "item": new_entry}

@app.delete("/api/history/{item_id}")
def delete_history(item_id: str):
    history = load_json_file(HISTORY_FILE, [])
    history = [h for h in history if h.get("id") != item_id]
    save_json_file(HISTORY_FILE, history)
    return {"status": "ok"}

@app.post("/api/feedback")
def submit_feedback(
    predicted_intent: str = Form(...),
    user_verdict: str = Form(...),
    correct_intent: Optional[str] = Form(None)
):
    entry = {
        "timestamp": datetime.now().isoformat(),
        "predicted_intent": predicted_intent,
        "user_verdict": user_verdict,
        "user_corrected_intent": correct_intent or "N/A"
    }
    df = pd.DataFrame([entry])
    if not os.path.exists(FEEDBACK_FILE):
        df.to_csv(FEEDBACK_FILE, index=False)
    else:
        df.to_csv(FEEDBACK_FILE, mode='a', header=False, index=False)
    return {"status": "ok", "message": "Feedback successfully logged"}

# نمونه صداهای آماده برای تست سریع
SAMPLE_FILES = [
    {
        "id": "food_sample",
        "title": "Hungry Meow (Food)",
        "label": "Food",
        "filename": "F_MIN01_EU_FN_BEN01_102.wav",
        "icon": "🍗",
        "cat": "MIN01 (Unseen Test Cat)"
    },
    {
        "id": "brushing_sample",
        "title": "Happy Trill (Brushing)",
        "label": "Brushing",
        "filename": "B_BRA01_MC_MN_SIM01_301.wav",
        "icon": "💆",
        "cat": "BRA01 (Unseen Test Cat)"
    },
    {
        "id": "isolation_sample",
        "title": "Distress Call (Isolation)",
        "label": "Isolation",
        "filename": "I_MIN01_EU_FN_BEN01_106.wav",
        "icon": "😿",
        "cat": "MIN01 (Unseen Test Cat)"
    }
]

@app.get("/api/samples")
def list_samples():
    return SAMPLE_FILES

@app.get("/api/sample/{filename}")
def stream_sample(filename: str):
    path = os.path.join(DATASET_DIR, filename)
    if not os.path.exists(path):
        raise HTTPException(status_code=404, detail="Sample file not found")
    return FileResponse(path, media_type="audio/wav")

# روت اصلی پردازش و استنتاج صدا
@app.post("/api/predict")
async def predict_voice(
    audio_file: Optional[UploadFile] = File(None),
    sample_filename: Optional[str] = Form(None),
    model_type: Optional[str] = Form(None)
):
    if not model_type:
        settings = load_json_file(SETTINGS_FILE, {})
        model_type = settings.get("model", "hybrid")

    audio_bytes = None

    if sample_filename:
        # بارگذاری از نمونه‌های تست موجود
        sample_path = os.path.join(DATASET_DIR, sample_filename)
        if not os.path.exists(sample_path):
            raise HTTPException(status_code=404, detail="فایل نمونه یافت نشد")
        with open(sample_path, "rb") as f:
            audio_bytes = f.read()
    elif audio_file:
        audio_bytes = await audio_file.read()
    else:
        raise HTTPException(status_code=400, detail="هیچ فایل صوتی ارسال نشد.")

    if not audio_bytes or len(audio_bytes) < 100:
        raise HTTPException(status_code=400, detail="فایل صوتی خالی یا نامعتبر است.")

    try:
        # خواندن صدا با librosa / soundfile
        bio = io.BytesIO(audio_bytes)
        try:
            y, sr = sf.read(bio)
            if y.ndim > 1:
                y = np.mean(y, axis=1)
            if sr != TARGET_SR:
                y = librosa.resample(y.astype(np.float32), orig_sr=sr, target_sr=TARGET_SR)
        except Exception:
            bio.seek(0)
            y, _ = librosa.load(bio, sr=TARGET_SR)

        if len(y) == 0:
            raise ValueError("صدا قابل دیکود نبود.")

        # استنتاج
        result = ai_engine.analyze_audio(y, model_type=model_type)
        return {"status": "ok", "result": result}

    except Exception as e:
        print("Prediction error:", e)
        raise HTTPException(status_code=500, detail=f"خطا در تحلیل صدا: {str(e)}")

# روت اصلی ارائه فرانت‌اند
@app.get("/", response_class=HTMLResponse)
def index_page():
    index_path = os.path.join(STATIC_DIR, "index.html")
    if not os.path.exists(index_path):
        return HTMLResponse("<h1>در حال بارگذاری سیستم... لطفاً چند لحظه صبر کنید.</h1>")
    with open(index_path, "r", encoding="utf-8") as f:
        return HTMLResponse(f.read())

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("server:app", host="0.0.0.0", port=7860, reload=True)
