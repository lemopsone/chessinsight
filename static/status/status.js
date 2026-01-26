(function () {
  const activeConnections = document.getElementById("activeConnections");
  const readingConnections = document.getElementById("readingConnections");
  const writingConnections = document.getElementById("writingConnections");
  const waitingConnections = document.getElementById("waitingConnections");
  const acceptedRequests = document.getElementById("acceptedRequests");
  const handledRequests = document.getElementById("handledRequests");
  const totalRequests = document.getElementById("totalRequests");
  const statusNote = document.getElementById("statusNote");
  const updatedAt = document.getElementById("updatedAt");
  const barReading = document.getElementById("barReading");
  const barWriting = document.getElementById("barWriting");
  const barWaiting = document.getElementById("barWaiting");

  function parseStatus(text) {
    const lines = text.trim().split(/\n+/);
    const activeMatch = lines[0] ? lines[0].match(/Active connections:\s*(\d+)/i) : null;
    const active = activeMatch ? Number(activeMatch[1]) : 0;

    const countsLine = lines.find((line) => /\d+\s+\d+\s+\d+/.test(line)) || "";
    const counts = countsLine.trim().split(/\s+/).map((item) => Number(item));

    const rwLine = lines.find((line) => /Reading:\s*\d+/i.test(line)) || "";
    const rwMatch = rwLine.match(/Reading:\s*(\d+)\s*Writing:\s*(\d+)\s*Waiting:\s*(\d+)/i);
    const reading = rwMatch ? Number(rwMatch[1]) : 0;
    const writing = rwMatch ? Number(rwMatch[2]) : 0;
    const waiting = rwMatch ? Number(rwMatch[3]) : 0;

    return {
      active,
      accepts: counts[0] || 0,
      handled: counts[1] || 0,
      requests: counts[2] || 0,
      reading,
      writing,
      waiting,
    };
  }

  function setText(el, value) {
    if (el) {
      el.textContent = value;
    }
  }

  function setBar(el, value, total) {
    if (!el) {
      return;
    }
    const percent = total === 0 ? 0 : Math.round((value / total) * 100);
    el.style.width = percent + "%";
  }

  function updateStatus() {
    fetch("/status/raw")
      .then((res) => {
        if (!res.ok) {
          throw new Error("Статус недоступен");
        }
        return res.text();
      })
      .then((text) => {
        const data = parseStatus(text);
        setText(activeConnections, data.active);
        setText(readingConnections, data.reading);
        setText(writingConnections, data.writing);
        setText(waitingConnections, data.waiting);
        setText(acceptedRequests, data.accepts);
        setText(handledRequests, data.handled);
        setText(totalRequests, data.requests);

        const totalConnections = data.reading + data.writing + data.waiting;
        setBar(barReading, data.reading, totalConnections);
        setBar(barWriting, data.writing, totalConnections);
        setBar(barWaiting, data.waiting, totalConnections);

        const updated = new Date().toLocaleTimeString();
        setText(updatedAt, "Последнее обновление: " + updated);
      })
      .catch((error) => {
        setText(statusNote, error.message);
      });
  }

  updateStatus();
  setInterval(updateStatus, 3000);
})();
