import {BASE_URL, CACHE_MODE, USE_STAGES} from "../config.js";
import http from 'k6/http';
import {check} from "k6";
import {randomInt} from "../utils.js";

export const constantOptions = {
    vus: 20,
    duration: "7s",
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

const size = 20;

export default function test () {

    const page =
        Math.random() < 0.9
            ? randomInt(0, 20)      // 90% горячие
            : randomInt(21, 200); // 10% холодные

    const res = http.get(`${BASE_URL}/clients?cacheMode=${CACHE_MODE}&page=${page}&size=${size}`);

    check(res, { "status is 200": (r) => r.status === 200 });
}