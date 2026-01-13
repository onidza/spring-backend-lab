export const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
export const CACHE_MODE = __ENV.CACHE_MODE || "NON_CACHE";

export const USE_STAGES = __ENV.USE_STAGES === "true";


//chocolately by administrator setup
/*

Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

choco install k6
*/
