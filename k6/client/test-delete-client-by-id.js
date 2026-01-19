import {BASE_URL, CACHE_MODE, USE_STAGES} from "../config.js";
import http from 'k6/http';
import {check} from "k6";
import {generatePhoneForSetup} from "../utils.js";

export const constantOptions = {
    vus: 20,
    duration: "10s",
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

//TRUNCATE TABLE
//setup pool
export function setup() {
    const ids = [];
    const count = 5_000;

    for (let i = 1; i <= count; i++) {
        const clientId = i;

        const phone = generatePhoneForSetup(clientId)
        const payload = JSON.stringify({
            name: `TestName${clientId}`,
            email: `test${clientId}@example.com`,
            profile: {
                address: `TestAddress${clientId}`,
                phone: phone,
            }
        });

        const res = http.post(
            `${BASE_URL}/clients?cacheMode=${CACHE_MODE}`,
            payload, {
                headers: {"Content-Type": "application/json"},
            });

        if (res.status === 201) ids.push(clientId);
    }

    console.log(`created: ${ids.length}/${count}`);

    return {ids};
}

export default function test(data) {

    if (!data.ids || data.ids.length === 0) {
        throw new Error("No client ids created in setup()");
    }

    const ids = data.ids;
    const vus = __ENV.VUS_MAX ? Number.parseInt(__ENV.VUS_MAX, 10) : 20;
    const perVu = Math.ceil(ids.length / vus);

    const start = (__VU - 1) * perVu;
    const idx = start + __ITER;

    if (idx >= ids.length) return;

    const clientId = ids[idx];

    const res = http.del(
        `${BASE_URL}/clients/${clientId}?cacheMode=${CACHE_MODE}`
    );

    const body = typeof res.body === "string"
        ? res.body
        : JSON.stringify(res.body);

    console.log(
        `status=${res.status}, error=${res.error || "none"}, body=${body.slice(0, 500)}`
    );

    check(res, {
            "status is ok":
                (r) => r.status === 200 || r.status === 204
        }
    );
}

//clearing the pool
export function teardown(data) {
    for (const id of data.ids) {
        http.del(`${BASE_URL}/clients/${id}?cacheMode=${CACHE_MODE}`);
    }
}