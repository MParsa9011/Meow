import os
import pandas as pd

files = os.listdir("dataset")

data = []

for file in files:
    parts = file.split("_")

    data.append({
        "context": parts[0],
        "cat_id": parts[1],
        "breed": parts[2],
        "gender": parts[3],
        "owner_id": parts[4],
        "recording_session": parts[5][0],
        "vocalization_counter": parts[5][1:3],
        "filename": file
    })

df = pd.DataFrame(data)

df.to_csv("metadata.csv", index=False)