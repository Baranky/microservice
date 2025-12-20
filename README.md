#  Spring Cloud Microservices Demo: Event-Driven Architecture

Bu proje, **Spring Boot 3** ve **Spring Cloud** teknolojileri kullanılarak geliştirilmiş, ölçeklenebilir ve hataya dayanıklı (fault-tolerant) bir mikroservis mimarisi örneğidir. Proje temel olarak Service Discovery, API Gateway, **Resilience4j ile Circuit Breaker**, **Event-Driven Architecture (Kafka)**, ve **Distributed Tracing (Zipkin)** desenlerini demonstrasyonunu içerir.

## Mimari ve Servisler

Proje 5 ana mikroservisten oluşur:

| Servis | Port   | Açıklama |
| :--- |:-------| :--- |
| **EurekaService** | `8761` | Service Discovery sunucusu. Tüm servislerin kayıt defteridir. |
| **ApiGateway** | `8082` | Dış dünyaya açılan tek kapı (Entry Point). Spring Cloud Gateway. |
| **ProductService** | `8092` | Ürün yönetim servisi. PostgreSQL veritabanı kullanır. Ürün oluşturulduğunda InventoryService'e stock bilgisini gönderir. |
| **OrderService** | `8093` | Sipariş servisi. Resilience4j Circuit Breaker içerir. Sipariş oluşturulduğunda Kafka'ya event gönderir. |
| **InventoryService** | `8094` | Stok yönetim servisi. PostgreSQL veritabanı kullanır. Kafka'dan sipariş event'lerini dinler ve stok düşürür. |

## İş Akışı (Event-Driven)

1. **Ürün Oluşturma:**
   - Kullanıcı `ProductService`'e ürün ekler (stock bilgisi ile)
   - `ProductService` ürünü kaydeder ve `InventoryService`'e Feign client ile stock bilgisini gönderir
   - `InventoryService` stock kaydını oluşturur

2. **Sipariş Oluşturma:**
   - Kullanıcı `OrderService`'e sipariş oluşturur
   - `OrderService` siparişi veritabanına kaydeder
   - `OrderService` Kafka'ya `OrderEvent` gönderir (orderId, productId, quantity)
   - `InventoryService` Kafka'dan event'i dinler ve stok düşürür

3. **Ürün Silme:**
   - `ProductService`'den ürün silindiğinde, `InventoryService`'den de otomatik olarak stock kaydı silinir

## Teknolojiler

* **Dil:** Java 21
* **Framework:** Spring Boot 3.5.x, Spring Cloud 2025.0.0
* **Discovery:** Netflix Eureka Client / Server
* **Gateway:** Spring Cloud Gateway
* **İletişim:** 
  - Spring Cloud OpenFeign (Senkron servis çağrıları)
  - Apache Kafka (Asenkron event-driven iletişim)
* **Resilience:** Spring Cloud Circuit Breaker (Resilience4j)
* **Tracing:** Zipkin (Distributed Tracing)
* **Veritabanı:** PostgreSQL, Spring Data JPA
* **Mesajlaşma:** Apache Kafka, Zookeeper
* **Araçlar:** Lombok, Maven, Docker Compose

##  Docker Compose Servisleri

Proje kök dizinindeki `docker-compose.yml` dosyası şu servisleri içerir:

| Servis | Port | Açıklama |
| :--- | :--- | :--- |
| **Zookeeper** | `2181` | Kafka için koordinasyon servisi |
| **Kafka** | `9092` | Event streaming platformu |
| **Kafka UI** | `8096` | Kafka topic'lerini görüntülemek için web arayüzü |
| **Zipkin** | `9411` | Distributed tracing UI |

##  Kurulum ve Çalıştırma

### 1. Docker Servislerini Başlat

```bash
docker compose up -d
```

Bu komut şunları başlatır:
- Zookeeper
- Kafka
- Kafka UI (http://localhost:8096)
- Zipkin (http://localhost:9411)



### 3.Servisleri Başlat (Sırayla)

1. **EurekaService** - Service Discovery
2. **ProductService** - Port 8092
3. **OrderService** - Port 8093
4. **InventoryService** - Port 8094
5. **ApiGateway** - Port 8082




## 📊 Monitoring ve Tracing

### Zipkin UI

Distributed tracing için Zipkin UI'ya erişin:
- URL: http://localhost:9411
- Tüm servisler arasındaki istek akışını görüntüleyebilirsiniz

### Kafka UI

Kafka topic'lerini ve mesajları görüntülemek için:
- URL: http://localhost:8096

##  API Endpoints

### ProductService

- `GET /api/products` - Tüm ürünleri listele
- `GET /api/products/{id}` - Ürün detayı
- `POST /api/products` - Yeni ürün oluştur (stock bilgisi ile)
- `PUT /api/products/{id}` - Ürün güncelle
- `DELETE /api/products/{id}` - Ürün sil (InventoryService'den de silinir)

### OrderService

- `GET /api/orders` - Tüm siparişleri listele
- `GET /api/orders/{id}` - Sipariş detayı
- `GET /api/orders/customer/{email}` - Müşteri siparişleri
- `POST /api/orders` - Yeni sipariş oluştur (Kafka'ya event gönderir)
- `PUT /api/orders/{id}` - Sipariş güncelle
- `DELETE /api/orders/{id}` - Sipariş sil



