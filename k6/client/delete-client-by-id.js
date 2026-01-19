import {randomInt} from "../utils.js";
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

    const clientId = randomInt(1, 2_000_000);

    const res = http.delete(
        `${BASE_URL}/clients/${clientId}?cacheMode=${CACHE_MODE}`
    );

    check(res, { "status is ok":
            (r) => r.status === 200 || r.status === 204}
    );
}