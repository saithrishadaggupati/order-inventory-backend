import http from 'k6/http';
import { check } from 'k6';

const TOKEN = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJsb2FkdGVzdGVyIiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3ODM2NTk3NDMsImV4cCI6MTc4MzY2MzM0M30.D3aJs-wwslJCd1_kQKUKeu0o5Q0GFEvmQoAKX1HjJQ8';
const PRODUCT_ID = 5;

export const options = {
    scenarios: {
        order_burst: {
            executor: 'shared-iterations',
            vus: 50,
            iterations: 100,
            maxDuration: '30s',
        },
    },
    // explicitly ask k6 for p99, not just the default p90/p95
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
    const url = 'http://localhost:8080/orders';
    const payload = JSON.stringify({ productId: PRODUCT_ID, quantity: 1 });
    const params = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${TOKEN}`,
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'order succeeded (200)': (r) => r.status === 200,
        'correctly rejected - out of stock or conflict (400/409)': (r) => r.status === 400 || r.status === 409,
    });
}