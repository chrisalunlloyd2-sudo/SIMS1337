import json, os, glob
d = r"C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\suggestions"
bad = []
total = 0
for f in glob.glob(os.path.join(d, "*.json")):
    total += 1
    try:
        with open(f, encoding="utf-8") as fh:
            json.load(fh)
    except Exception as e:
        bad.append((os.path.basename(f), str(e)[:120]))
print(f"total={total} bad={len(bad)}")
for name, err in bad[:20]:
    print(f"BAD {name}: {err}")