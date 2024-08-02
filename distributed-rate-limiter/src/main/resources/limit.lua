-- 获取限流的键和限流大小
local key = KEYS[1]
local limit = tonumber(ARGV[1])
-- 获取过期时间
local expire_time = tonumber(ARGV[2])

-- 获取当前的请求数量，如果键不存在则为0
local current = tonumber(redis.call('get', key) or "0")

if current + 1 > limit then
    -- 如果当前请求数量加1超过限制大小，返回0表示请求被拒绝
    return 0
else
    -- 使用一个事务来保证原子性
    redis.call("INCRBY", key, "1")
    redis.call("EXPIRE", key, expire_time)
    -- 如果需要调整时间单位为 ms
    -- redis.call("PEXPIRE", key, expire_time)
    return 1
end
