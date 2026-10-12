import os
import numpy as np
import librosa
import scipy.signal as signal
from scipy.stats import skew

TARGET_SR = 8000

def denoise_audio(y, sr=TARGET_SR):
    """
    Audio preprocessing and noise reduction:
    1. DC offset removal
    2. 2nd-order Butterworth high-pass filter at 70 Hz: eliminates microphone handling rumble,
       room resonance, and AC electrical hum (50/60 Hz) without affecting cat vocal range (150-2000+ Hz).
    3. Intelligent voice activity trimming to remove leading and trailing background silence.
    """
    if len(y) == 0:
        return y
    # 1. DC offset removal
    y_clean = y - np.mean(y)

    # 2. High-pass filter at 70 Hz
    try:
        nyq = 0.5 * sr
        b, a = signal.butter(2, 70.0 / nyq, btype='high')
        y_clean = signal.filtfilt(b, a, y_clean)
    except Exception:
        pass

    # 3. Trim silence
    try:
        y_trim, _ = librosa.effects.trim(y_clean, top_db=25)
        if len(y_trim) >= int(sr * 0.25):
            y_clean = y_trim
    except Exception:
        pass

    return y_clean

def extract_audio_features(y, sr=TARGET_SR):
    """
    Extract comprehensive 234 acoustic & bioacoustic features from cat vocalizations.
    Includes:
    - Base Temporal & Energy features (duration, RMS stats, zero crossing rate)
    - Bioacoustic energy dynamics (RMS peak position, post-peak decay slope)
    - Spectral characteristics (centroid, bandwidth, rolloffs at 25%, 50%, 85%, 95%, flatness, contrast, flux)
    - MFCCs (20 coefficients) + 1st & 2nd order velocity and acceleration deltas
    - Pitch / Fundamental Frequency (F0) dynamics using YIN (mean, median, std, min, max, range, IQR, voiced ratio, slope, curvature, jitter)
    - Chroma pitch classes (12 semitones)
    """
    raw_duration = len(y) / sr

    # Base trim for spectral consistency
    y_trimmed, _ = librosa.effects.trim(y, top_db=25)
    if len(y_trimmed) < int(sr * 0.2):
        y_trimmed = y

    # Denoised audio for noise-sensitive dynamics
    y_clean = denoise_audio(y, sr=sr)

    feats = {}

    # --- 1. ویژگی‌های زمانی و انرژی (Temporal & Energy Profile) ---
    duration = len(y_trimmed) / sr
    feats['duration'] = float(duration)
    feats['duration_ratio'] = float(duration / (raw_duration + 1e-6))

    rms = librosa.feature.rms(y=y_trimmed)[0]
    feats['rms_mean'] = float(np.mean(rms))
    feats['rms_std'] = float(np.std(rms))
    feats['rms_max'] = float(np.max(rms))
    feats['rms_skew'] = float(skew(rms)) if len(rms) > 3 else 0.0

    zcr = librosa.feature.zero_crossing_rate(y=y_trimmed)[0]
    feats['zcr_mean'] = float(np.mean(zcr))
    feats['zcr_std'] = float(np.std(zcr))

    # --- 2. ویژگی‌های طیفی پایه (Spectral Features) ---
    cent = librosa.feature.spectral_centroid(y=y_trimmed, sr=sr)[0]
    feats['cent_mean'] = float(np.mean(cent))
    feats['cent_std'] = float(np.std(cent))
    feats['cent_min'] = float(np.min(cent))
    feats['cent_max'] = float(np.max(cent))

    bw = librosa.feature.spectral_bandwidth(y=y_trimmed, sr=sr)[0]
    feats['bw_mean'] = float(np.mean(bw))
    feats['bw_std'] = float(np.std(bw))

    rolloff_85 = librosa.feature.spectral_rolloff(y=y_trimmed, sr=sr, roll_percent=0.85)[0]
    feats['rolloff85_mean'] = float(np.mean(rolloff_85))
    feats['rolloff85_std'] = float(np.std(rolloff_85))

    rolloff_95 = librosa.feature.spectral_rolloff(y=y_trimmed, sr=sr, roll_percent=0.95)[0]
    feats['rolloff95_mean'] = float(np.mean(rolloff_95))
    feats['rolloff95_std'] = float(np.std(rolloff_95))

    flatness = librosa.feature.spectral_flatness(y=y_trimmed)[0]
    feats['flatness_mean'] = float(np.mean(flatness))
    feats['flatness_std'] = float(np.std(flatness))

    contrast = librosa.feature.spectral_contrast(y=y_trimmed, sr=sr, fmin=100.0, n_bands=4)
    for i in range(contrast.shape[0]):
        feats[f'contrast_mean_{i}'] = float(np.mean(contrast[i]))
        feats[f'contrast_std_{i}'] = float(np.std(contrast[i]))

    # --- 3. ضرایب مپ‌استرال (MFCCs 1-20 + Deltas + Delta-Deltas) ---
    hop_length = 256
    n_fft = 512
    mfcc = librosa.feature.mfcc(y=y_trimmed, sr=sr, n_mfcc=20, n_fft=n_fft, hop_length=hop_length)
    width = 9 if mfcc.shape[1] >= 9 else (mfcc.shape[1] if mfcc.shape[1] % 2 == 1 else mfcc.shape[1] - 1)
    if width >= 3:
        mfcc_delta = librosa.feature.delta(mfcc, width=width)
        mfcc_delta2 = librosa.feature.delta(mfcc, order=2, width=width)
    else:
        mfcc_delta = np.zeros_like(mfcc)
        mfcc_delta2 = np.zeros_like(mfcc)

    for i in range(20):
        feats[f'mfcc_mean_{i}'] = float(np.mean(mfcc[i]))
        feats[f'mfcc_std_{i}'] = float(np.std(mfcc[i]))
        feats[f'mfcc_max_{i}'] = float(np.max(mfcc[i]))
        feats[f'mfcc_min_{i}'] = float(np.min(mfcc[i]))
        feats[f'mfcc_delta_mean_{i}'] = float(np.mean(mfcc_delta[i]))
        feats[f'mfcc_delta_std_{i}'] = float(np.std(mfcc_delta[i]))
        feats[f'mfcc_delta2_mean_{i}'] = float(np.mean(mfcc_delta2[i]))
        feats[f'mfcc_delta2_std_{i}'] = float(np.std(mfcc_delta2[i]))

    # --- 4. فرکانس پایه (F0 / Pitch Contour & Dynamics) ---
    try:
        y_f0 = np.ascontiguousarray(y_trimmed, dtype=np.float64)
        f0 = librosa.yin(y_f0, fmin=150, fmax=2000, sr=sr)
        valid_f0 = f0[(f0 >= 160) & (f0 <= 1950)]
        if len(valid_f0) > 3:
            feats['f0_mean'] = float(np.mean(valid_f0))
            feats['f0_std'] = float(np.std(valid_f0))
            feats['f0_min'] = float(np.min(valid_f0))
            feats['f0_max'] = float(np.max(valid_f0))
            feats['f0_range'] = float(feats['f0_max'] - feats['f0_min'])
            feats['f0_voiced_ratio'] = float(len(valid_f0) / len(f0))
            x_idx = np.arange(len(valid_f0))
            feats['f0_slope'] = float(np.polyfit(x_idx, valid_f0, 1)[0])
        else:
            feats['f0_mean'] = 0.0
            feats['f0_std'] = 0.0
            feats['f0_min'] = 0.0
            feats['f0_max'] = 0.0
            feats['f0_range'] = 0.0
            feats['f0_voiced_ratio'] = 0.0
            feats['f0_slope'] = 0.0
    except Exception:
        feats['f0_mean'] = 0.0
        feats['f0_std'] = 0.0
        feats['f0_min'] = 0.0
        feats['f0_max'] = 0.0
        feats['f0_range'] = 0.0
        feats['f0_voiced_ratio'] = 0.0
        feats['f0_slope'] = 0.0

    # --- 5. رده‌های کرومایی (Chroma Pitch Classes) ---
    chroma = librosa.feature.chroma_stft(y=y_trimmed, sr=sr)
    for i in range(12):
        feats[f'chroma_mean_{i}'] = float(np.mean(chroma[i]))
        feats[f'chroma_std_{i}'] = float(np.std(chroma[i]))

    # --- 6. ویژگی‌های بایوآکوستیک حاصل از سیگنال پالایش‌شده (Denoised Bioacoustic Dynamics) ---
    # الف) دینامیک اوج و شیب افت انرژی
    rms_c = librosa.feature.rms(y=y_clean, hop_length=128)[0]
    peak_idx = np.argmax(rms_c) if len(rms_c) > 0 else 0
    feats['rms_peak_pos'] = float(peak_idx / max(1, len(rms_c)))
    if peak_idx < len(rms_c) - 2:
        decay_seg = rms_c[peak_idx:]
        feats['rms_decay_slope'] = float(np.polyfit(np.arange(len(decay_seg)), decay_seg, 1)[0])
    else:
        feats['rms_decay_slope'] = 0.0

    # ب) شار طیفی و شدت برانگیختگی (Spectral Flux / Onset Strength)
    onset_env = librosa.onset.onset_strength(y=y_clean, sr=sr, hop_length=128)
    feats['flux_mean'] = float(np.mean(onset_env))
    feats['flux_std'] = float(np.std(onset_env))
    feats['flux_max'] = float(np.max(onset_env))

    # ج) رول‌آف در صدک‌های ۲۵ و ۵۰ درصد (Multi-percentile Rolloffs)
    ro_25 = librosa.feature.spectral_rolloff(y=y_clean, sr=sr, roll_percent=0.25, hop_length=128)[0]
    feats['rolloff_25_mean'] = float(np.mean(ro_25))
    feats['rolloff_25_std'] = float(np.std(ro_25))

    ro_50 = librosa.feature.spectral_rolloff(y=y_clean, sr=sr, roll_percent=0.50, hop_length=128)[0]
    feats['rolloff_50_mean'] = float(np.mean(ro_50))
    feats['rolloff_50_std'] = float(np.std(ro_50))

    # د) ویژگی‌های پیشرفته پیچ (Median, IQR, Curvature, Jitter)
    try:
        y_clean_f0 = np.ascontiguousarray(y_clean, dtype=np.float64)
        f0_c = librosa.yin(y_clean_f0, fmin=150, fmax=2000, sr=sr)
        valid_f0_c = f0_c[(f0_c >= 160) & (f0_c <= 1950)]
        if len(valid_f0_c) > 4:
            feats['f0_median'] = float(np.median(valid_f0_c))
            feats['f0_iqr'] = float(np.percentile(valid_f0_c, 75) - np.percentile(valid_f0_c, 25))
            x_idx = np.linspace(0, 1, len(valid_f0_c))
            poly2 = np.polyfit(x_idx, valid_f0_c, 2)
            feats['f0_curvature'] = float(poly2[0])
            diff_f0 = np.abs(np.diff(valid_f0_c))
            feats['f0_jitter'] = float(np.mean(diff_f0) / (np.mean(valid_f0_c) + 1e-6))
        else:
            feats['f0_median'] = 0.0
            feats['f0_iqr'] = 0.0
            feats['f0_curvature'] = 0.0
            feats['f0_jitter'] = 0.0
    except Exception:
        feats['f0_median'] = 0.0
        feats['f0_iqr'] = 0.0
        feats['f0_curvature'] = 0.0
        feats['f0_jitter'] = 0.0

    return feats

def extract_features_from_file(file_path, sr=TARGET_SR):
    """Load audio from file and extract acoustic feature vector."""
    y, _ = librosa.load(file_path, sr=sr)
    return extract_audio_features(y, sr=sr)
