# 1. Используем лёгкий Node образ
FROM node:20-alpine

# 2. Рабочая директория внутри контейнера
WORKDIR /app

# 3. Копируем package.json и package-lock.json (или yarn.lock)
COPY package*.json ./

# 4. Устанавливаем зависимости
RUN npm install

# 5. Копируем остальные файлы проекта
COPY . .

# 6. Команда запуска dev-сервера Vite
# --host нужно, чтобы сервер был доступен снаружи контейнера
CMD ["npm", "run", "dev", "--", "--host"]