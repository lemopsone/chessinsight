FROM python:3.12-alpine

WORKDIR /app
RUN pip install --no-cache-dir python-chess
COPY mock_stockfish_server.py /app/mock_stockfish_server.py

EXPOSE 5555
CMD ["python", "/app/mock_stockfish_server.py"]
