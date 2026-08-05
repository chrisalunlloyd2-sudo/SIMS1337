import os
import sqlite3
import hashlib
from datetime import datetime

DB_DIR = r"C:\Users\viper\local_desktop_main\code_registry"
DB_PATH = os.path.join(DB_DIR, "code_registry.db")
ROOT_DIR = r"C:\Users\viper\local_desktop_main"

def init_db():
    if not os.path.exists(DB_DIR):
        os.makedirs(DB_DIR)
    
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS code_pages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, 
                    filepath TEXT UNIQUE NOT NULL, 
                    language TEXT, 
                    hash_sha256 TEXT, 
                    lines INTEGER, 
                    created_at TEXT DEFAULT CURRENT_TIMESTAMP, 
                    last_modified TEXT, 
                    description TEXT, 
                    tags TEXT, 
                    module TEXT)''')
    
    c.execute('''CREATE TABLE IF NOT EXISTS code_changelog (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, 
                    filepath TEXT, 
                    action TEXT, 
                    diff_summary TEXT, 
                    timestamp TEXT DEFAULT CURRENT_TIMESTAMP, 
                    phase TEXT)''')
    
    c.execute('''CREATE INDEX IF NOT EXISTS idx_filepath ON code_pages(filepath)''')
    conn.commit()
    return conn

def get_language(filepath):
    ext = os.path.splitext(filepath)[1].lower()
    if ext == '.java': return 'Java'
    if ext == '.py': return 'Python'
    if ext == '.js': return 'JavaScript'
    if ext == '.fxml': return 'FXML'
    if ext == '.html': return 'HTML'
    if ext == '.md': return 'Markdown'
    if ext == '.xml': return 'XML'
    return 'Unknown'

def scan_and_register(conn):
    c = conn.cursor()
    
    target_extensions = {'.java', '.py', '.js', '.fxml', '.html'}
    
    for root, _, files in os.walk(ROOT_DIR):
        if '.git' in root or 'node_modules' in root or 'target' in root:
            continue
            
        for file in files:
            ext = os.path.splitext(file)[1].lower()
            if ext in target_extensions:
                filepath = os.path.join(root, file)
                
                try:
                    with open(filepath, 'rb') as f:
                        content = f.read()
                        hash_sha256 = hashlib.sha256(content).hexdigest()
                        lines = len(content.splitlines())
                except Exception as e:
                    continue
                    
                lang = get_language(filepath)
                rel_path = os.path.relpath(filepath, ROOT_DIR)
                
                c.execute("SELECT hash_sha256 FROM code_pages WHERE filepath=?", (rel_path,))
                row = c.fetchone()
                
                if row is None:
                    c.execute('''INSERT INTO code_pages (filepath, language, hash_sha256, lines, last_modified)
                                 VALUES (?, ?, ?, ?, ?)''', (rel_path, lang, hash_sha256, lines, datetime.now().isoformat()))
                    c.execute('''INSERT INTO code_changelog (filepath, action, phase) VALUES (?, ?, ?)''', 
                                (rel_path, 'ADDED', 'PHASE_1C'))
                elif row[0] != hash_sha256:
                    c.execute('''UPDATE code_pages SET hash_sha256=?, lines=?, last_modified=? WHERE filepath=?''',
                                 (hash_sha256, lines, datetime.now().isoformat(), rel_path))
                    c.execute('''INSERT INTO code_changelog (filepath, action, phase) VALUES (?, ?, ?)''', 
                                (rel_path, 'UPDATED', 'PHASE_1C'))
    
    conn.commit()

if __name__ == '__main__':
    conn = init_db()
    scan_and_register(conn)
    conn.close()
    print("Database updated.")
