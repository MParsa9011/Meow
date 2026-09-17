import pandas as pd

df = pd.read_csv('metadata.csv')


train_df = df[~df["cat_id"].isin(["BAC01", "BRA01", "MIN01", "REG01"])]
test_df = df[df["cat_id"].isin(["BAC01", "BRA01", "MIN01", "REG01"])]

train_df.to_csv("train.csv", index=False)
test_df.to_csv("test.csv", index=False)