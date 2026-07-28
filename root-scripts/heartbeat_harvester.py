#!/usr/bin/env python3
"""Heartbeat Harvester — correlates old project ideas to current active projects."""

import json, os, re, sys
from datetime import datetime

SOV_PATH = "/root/sov"
KV_PATH = os.path.join(SOV_PATH, "kv", "data.json")
KG_PATH = os.path.join(SOV_PATH, "kg", "entities.json")

# Current active projects (from memory)
ACTIVE_PROJECTS = [
    "SIMS1337", "ViperKernel", "Plane2d", "MoeGUI", "ArchivalMoe",
    "GeneticFoundry", "ViperNote", "brute-foundry", "mind-palace",
    "living-ascii-art", "aegis-agent-bridge", "MatrixWinCE"
]

# Project keywords for correlation
PROJECT_KEYWORDS = {
    "SIMS1337": ["autonomous", "pipeline", "voting", "mining", "deploy", "lora", "gui", "java", "javafx"],
    "ViperKernel": ["kernel", "tensor", "gguf", "quantization", "attention", "simd"],
    "brute-foundry": ["brute", "foundry", "combinatorial", "deterministic", "ast", "symbolic"],
    "mind-palace": ["mind", "palace", "memory", "topological", "hex", "homology"],
    "living-ascii-art": ["ascii", "art", "dashboard", "analytics", "cloudflare"],
    "aegis-agent-bridge": ["agent", "bridge", "vote", "inbox", "poll"],
    "MatrixWinCE": ["matrix", "wince", "godhand", "orchestrator"],
}

def load_kv():
    try:
        with open(KV_PATH) as f:
            return json.load(f)
    except: return {}

def load_kg():
    try:
        with open(KG_PATH) as f:
            data = json.load(f)
            return data.get("entities", [])
    except: return []

def get_keywords():
    kv = load_kv()
    keywords = kv.get("global_keywords", [])
    if isinstance(keywords, str):
        keywords = json.loads(keywords)
    return keywords if isinstance(keywords, list) else []

def get_insights():
    kv = load_kv()
    insights = kv.get("nightly_insights", {})
    if isinstance(insights, str):
        insights = json.loads(insights)
    return insights if isinstance(insights, dict) else {}

def correlate():
    keywords = get_keywords()
    insights = get_insights()
    
    results = []
    
    # For each active project, check if any keywords match
    for project in ACTIVE_PROJECTS:
        pkw = PROJECT_KEYWORDS.get(project, [project.lower()])
        matches = [kw for kw in keywords if any(pk.lower() in kw.lower() for pk in pkw)]
        
        if matches:
            # Check if this is a new correlation (not in insights)
            themes = insights.get("themes", [])
            is_new = not any(project.lower() in t.lower() for t in themes)
            
            results.append({
                "project": project,
                "matches": len(matches),
                "keywords": matches[:5],  # top 5
                "is_new": is_new
            })
    
    return results

def main():
    results = correlate()
    
    if not results:
        print("🌾 Harvest: No correlations found this cycle.")
        return
    
    print(f"🌾 Harvest Report — {datetime.now().strftime('%Y-%m-%d %H:%M')}")
    print(f"   Projects with keyword matches: {len(results)}")
    
    new_correlations = [r for r in results if r.get("is_new")]
    if new_correlations:
        print(f"\n   🆕 New correlations:")
        for r in new_correlations:
            print(f"      {r['project']}: {r['matches']} keyword matches")
            for kw in r['keywords'][:3]:
                print(f"         - {kw}")
    
    # If anything is new and significant, flag for email
    if new_correlations:
        print("\n   ⚠️  New correlations detected — flag for morning digest.")
        # Write a flag file for the morning digest to pick up
        flag_path = os.path.join(SOV_PATH, "harvest_flag.json")
        with open(flag_path, 'w') as f:
            json.dump({
                "timestamp": datetime.now().isoformat(),
                "new_correlations": new_correlations,
                "total": len(results)
            }, f)

if __name__ == "__main__":
    main()
