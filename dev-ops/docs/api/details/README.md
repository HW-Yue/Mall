# 详细接口文档索引

这里记录每个服务的详细 HTTP 接口说明，包含：

- 请求参数
- 路径参数 / 查询参数
- 请求体样例
- 响应体样例
- 常见备注

## 通用响应结构

大部分服务统一返回：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {}
}
```

字段说明：

- `code`：业务码，`0000` 通常表示成功
- `info`：业务信息
- `data`：响应数据，可能是对象、列表、布尔值或字符串

## 服务文档

- [mall](./mall.md)
- [order-service](./order-service.md)
- [group-buy-service](./group-buy-service.md)
- [seckill-service](./seckill-service.md)
- [pay](./pay.md)
- [springcloud-gateway](./springcloud-gateway.md)
