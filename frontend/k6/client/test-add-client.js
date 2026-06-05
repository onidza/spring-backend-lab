import {generatePhone, generateUniqueClientId} from "../utils.js";
import {BASE_URL, CACHE_MODE, USE_STAGES} from "../config.js";
import http from 'k6/http';
import {check} from "k6";

export const constantOptions = {
    vus: 20,
    duration: "30s",
};

export const rampingOptions = {
    stages: [
        {duration: '5s', target: 400},
        {duration: '15s', target: 400},
        {duration: '5s', target: 0},
    ],
};

export const loadOptions = USE_STAGES ? rampingOptions : constantOptions;

export const options = {
    ...loadOptions,
    thresholds: {
        checks: ["rate>0.99"],
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<500"],
    },
};

export default function test() {

    const clientId = generateUniqueClientId();
    const phone = generatePhone();
    const payload = JSON.stringify({
        id: clientId,
        name: `TestName${clientId}-${__ITER}`,
        email: `test${clientId}-${__ITER}@example.com`,
        profile: {
            id: clientId,
            address: `TestAddress${clientId}-${__ITER}`,
            phone: phone,
            clientId: clientId
        }

    })

    const res = http.post(`${BASE_URL}/clients?cacheMode=${CACHE_MODE}`, payload, {
        headers: {'Content-Type': 'application/json'},
    });

    check(res, {"status is 201": (r) => r.status === 201});
}