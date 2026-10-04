import os
import numpy as np
import librosa
from scipy.stats import skew

TARGET_SR = 8000

def extract_audio_features(y, sr=TARGET_SR):
    """
    Extract a comprehensive set of 221 acoustic features from a cat vocalization audio.
    Includes:
    - Temporal & Energy features (duration, RMS stats, zero crossing rate)
    - Spectral features (centroid, bandwidth, rolloff 85/95, flatness, contrast)
    - MFCCs (20 coefficients) + 1st & 2nd order deltas
    - Pitch / Fundamental Frequency (F0) using YIN
    - Chroma pitch class features
    """
    # Trim silence
    y_trimmed, _ = librosa.effects.trim(y, top_db=25)
    if len(y_trimmed) < int(sr * 0.2):
        y_trimmed = y

    feats = {}

    # 1. Temporal & Energy Features
    duration = len(y_trimmed) / sr
    total_duration = len(y) / sr
    feats['duration'] = float(duration)
    feats['duration_ratio'] = float(duration / (total_duration + 1e-6))

    rms = librosa.feature.rms(y=y_trimmed)[0]
    feats['rms_mean'] = float(np.mean(rms))
    feats['rms_std'] = float(np.std(rms))
    feats['rms_max'] = float(np.max(rms))
    feats['rms_min'] = float(np.min(rms))
    feats['rms_skew'] = float(skew(rms)) if len(rms) > 3 else 0.0

    zcr = librosa.feature.zero_crossing_rate(y=y_trimmed)[0]
    feats['zcr_mean'] = float(np.mean(zcr))
    feats['zcr_std'] = float(np.std(zcr))

    # 2. Spectral Features
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

    # 3. MFCC (20 coefficients) + 1st and 2nd order deltas
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

    # 4. Fundamental Frequency (F0 / Pitch Contour)
    try:
        f0 = librosa.yin(y_trimmed, fmin=150, fmax=2000, sr=sr)
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

    # 5. Chroma Pitch Classes (12 semitones)
    chroma = librosa.feature.chroma_stft(y=y_trimmed, sr=sr)
    for i in range(12):
        feats[f'chroma_mean_{i}'] = float(np.mean(chroma[i]))
        feats[f'chroma_std_{i}'] = float(np.std(chroma[i]))

    return feats

def extract_features_from_file(file_path, sr=TARGET_SR):
    """Load audio from file and extract acoustic feature vector."""
    y, _ = librosa.load(file_path, sr=sr)
    return extract_audio_features(y, sr=sr)
