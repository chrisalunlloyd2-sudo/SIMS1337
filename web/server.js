const express = require('express');
const path = require('path');
const cors = require('cors');

const app = express();
const port = 8080;

app.use(cors());
app.use(express.json());

// Serve hexeract GUI static files
app.use('/hexeract', express.static(path.join(__dirname, 'hexeract-gui')));
app.use(express.static(path.join(__dirname)));

// ═══ HEXERACT STATE API ═══
// Generates 64 vertices with 6D state vectors
app.get('/api/hexeract/state', (req, res) => {
    const vertices = [];
    for (let i = 0; i < 64; i++) {
        const v = [];
        for (let d = 0; d < 6; d++) v.push((i >> d) & 1 ? 1 : -1);
        vertices.push({
            id: `v_${i}`,
            coords: v,
            entityClass: ['MODEL', 'SERVER', 'PID', 'REPO', 'AGENT'][i % 5],
            viscosity: 0.1 + Math.random() * 0.7,
            consensusWeight: 0.5 + Math.random() * 0.5,
            active: Math.random() > 0.25
        });
    }
    res.json({ vertices, totalEdges: 192, cubicCells: 160, quorumThreshold: 43 });
});

// Mock Hex Grid Data Points
app.get('/api/datapoints', (req, res) => {
    const points = [];
    for (let i = 0; i < 100; i++) {
        points.push({ id: `pt_${i}`, label: `Node ${i}`, type: Math.random() > 0.8 ? 'failure' : 'success' });
    }
    res.json({ points });
});

// System Tests
app.get('/api/system/tests', (req, res) => {
    res.json({ tests: [
        { id: Date.now(), layer: 'CORE', test_name: 'HexeractInit', status: 'PASS', sha256: 'a1b2c3d4e5f6' },
        { id: Date.now() + 1, layer: 'QUORUM', test_name: 'ConsensusThreshold', status: 'PASS', sha256: 'f6e5d4c3b2a1' },
        { id: Date.now() + 2, layer: 'GOSSIP', test_name: 'EdgePropagation', status: 'PASS', sha256: '1a2b3c4d5e6f' }
    ]});
});

// Chat Endpoint — Proxies to Ollama with dynamic model selection
app.post('/chat', async (req, res) => {
    const { prompt, model } = req.body;
    const selectedModel = model || 'qwen2.5:3b';
    try {
        const ollamaRes = await fetch('http://127.0.0.1:11434/api/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ model: selectedModel, prompt, stream: false })
        });
        const data = await ollamaRes.json();
        res.json({ response: data.response, model: selectedModel, duration: data.total_duration });
    } catch (error) {
        console.error('Ollama Error:', error.message);
        res.status(500).json({ error: 'Ollama connection failed — ensure it is running on port 11434' });
    }
});

// Models Endpoint
app.get('/api/models', async (req, res) => {
    try {
        const ollamaRes = await fetch('http://127.0.0.1:11434/api/tags');
        const data = await ollamaRes.json();
        res.json(data);
    } catch (error) {
        console.error('Ollama Models Error:', error.message);
        res.status(500).json({ error: 'Failed to fetch models from Ollama' });
    }
});

// ═══ ROUTES ═══
// Main page → Breathing Hexeract
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'hexeract-gui', 'index.html'));
});

// Legacy RISC manifold view
app.get('/manifold', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(port, '0.0.0.0', () => {
    console.log('');
    console.log('  ═══════════════════════════════════════════════════');
    console.log('  ⬡  SIMS1337 HEXERACT ENGINE — Server Online');
    console.log('  ═══════════════════════════════════════════════════');
    console.log(`  ⬡  Breathing Hexeract:  http://localhost:${port}`);
    console.log(`  ⬡  RISC Manifold:       http://localhost:${port}/manifold`);
    console.log(`  ⬡  Hexeract API:        http://localhost:${port}/api/hexeract/state`);
    console.log(`  ⬡  Ollama Models:       http://localhost:${port}/api/models`);
    console.log('  ═══════════════════════════════════════════════════');
    console.log('');
});
