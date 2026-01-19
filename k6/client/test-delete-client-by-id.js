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

//setup
export function setup() {
    const ids = [];
    const count = 2000;

    for (let i = 0; i < count; i++) {
        const clientId = 2_000_000 + i;

        const payload = JSON.stringify({
            client: {
                id: clientId,
                name: `TestName${clientId}`,
                email: `test${clientId}@example.com`,
                profile: {id: clientId, address: `TestAddress${clientId}`, clientId},
            },
        });

        const res = http.post(`${BASE_URL}/clients/`, payload, {
            headers: {"Content-Type": "application/json"},
        });

        if (res.status === 200 || res.status === 201) ids.push(clientId);
    }

    return {ids};
}

export default function (data) {

    const idx = (__VU * 1_000_000 + __ITER) % data.ids.length;
    const clientId = data.ids[idx];

    const res = http.delete(
        `${BASE_URL}/clients/${clientId}?cacheMode=${CACHE_MODE}`
    );

    check(res, {
            "status is ok":
                (r) => r.status === 200 || r.status === 204
        }
    );
}

//clearing
export function teardown(data) {
    for (const id of data.ids) {
        http.delete(`${BASE_URL}/clients/${id}?cacheMode=${CACHE_MODE}`);
    }
}