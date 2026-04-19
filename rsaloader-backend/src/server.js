const express = require('express');
const fs      = require('fs');
const path    = require('path');

const app  = express();
const PORT = process.env.PORT || 3000;

app.get('/v1/mod', (req, res) => {
    const modJarPath = path.join(__dirname, '..', 'mod.jar');
    if (!fs.existsSync(modJarPath)) {
        console.error('[SERVER] mod.jar not found at ' + modJarPath);
        return res.status(404).json({ error: 'mod.jar not found' });
    }
    console.log('[SERVER] Serving mod.jar to ' + req.ip);
    res.setHeader('Content-Type', 'application/java-archive');
    res.sendFile(modJarPath);
});

app.listen(PORT, () => console.log('[SERVER] RSALoader backend running on port ' + PORT));
