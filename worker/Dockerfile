FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY eda_worker.py .

EXPOSE 5000

CMD ["python", "eda_worker.py"]
