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