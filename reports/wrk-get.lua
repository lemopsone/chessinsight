local threads = {}

local function header_value(headers, name)
  return headers[name] or headers[string.lower(name)] or "unknown"
end

function setup(thread)
  thread:set("upstream_counts", {})
  thread:set("group_counts", {})
  table.insert(threads, thread)
end

function response(status, headers, body)
  local upstream = header_value(headers, "X-Upstream-Addr")
  local group = header_value(headers, "X-Upstream-Group")

  local uc = upstream_counts
  uc[upstream] = (uc[upstream] or 0) + 1

  local gc = group_counts
  gc[group] = (gc[group] or 0) + 1
end

local function merge_counts(dst, src)
  for k, v in pairs(src) do
    dst[k] = (dst[k] or 0) + v
  end
end

local function print_counts(title, counts, total)
  print(title)
  local keys = {}
  for k in pairs(counts) do table.insert(keys, k) end
  table.sort(keys)
  for _, k in ipairs(keys) do
    local c = counts[k]
    local pct = (total > 0) and (c * 100.0 / total) or 0
    print(string.format("  %s = %d (%.2f%%)", k, c, pct))
  end
end

function done(summary, latency, requests)
  local upstream_total = {}
  local group_total = {}

  for _, thread in ipairs(threads) do
    merge_counts(upstream_total, thread:get("upstream_counts"))
    merge_counts(group_total, thread:get("group_counts"))
  end

  print("")
  print("Upstream distribution (from response headers):")
  print_counts("- Upstream addr:", upstream_total, summary.requests)
  print_counts("- Upstream group:", group_total, summary.requests)
end
