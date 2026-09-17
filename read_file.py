import os

files = os.listdir("dataset")
files_part = []

context = []
cat_id = []
breeds = []
gender = []
owner_id = []
recording_session = []
vocalization_counter = []


for i in range(len(files)):
    files_part.append(files[i].split("_"))
    context.append(files_part[i][0])
    cat_id.append(files_part[i][1])
    breeds.append(files_part[i][2])
    gender.append(files_part[i][3])
    owner_id.append(files_part[i][4])
    recording_session.append(files_part[i][5][0])
    vocalization_counter.append(files_part[i][5][1:3])

    
print(recording_session)
print(vocalization_counter)

