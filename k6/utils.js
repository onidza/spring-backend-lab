export function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

export function generateRandomIds(count, minId, maxId) {
    const ids = [];
    for (let i = 0; i < count; i++) {
        ids.push(randomInt(minId, maxId));
    }
    return ids;
}

export function generatePhoneForSetup(i, base = "+79") {
    return `${base}${String(i).padStart(9, "0")}`;
}

export function generatePhone(base = "+79") {
    return `${base}${String(__VU).padStart(3,'0')}${String(__ITER).padStart(6,'0')}`;
}

export function generateUniqueClientId(base = 2_000_000, stride = 1_000_000) {
    return base + (__VU * stride) + __ITER;
}