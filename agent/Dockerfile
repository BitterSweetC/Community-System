# syntax=docker/dockerfile:1.7
FROM python:3.10-slim

WORKDIR /app
ENV PIP_DISABLE_PIP_VERSION_CHECK=1

# Install system dependencies if needed (e.g. for building packages)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements
COPY requirements.txt .

# Install python dependencies
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt

# Copy application code
COPY . .

# Expose port
EXPOSE 8000

# Run application
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
