# 🚀 Spring Cloud Microservices Demo: Circuit Breaker Pattern

Bu proje, **Spring Boot 3** ve **Spring Cloud** teknolojileri kullanılarak geliştirilmiş, ölçeklenebilir ve hataya dayanıklı (fault-tolerant) bir mikroservis mimarisi örneğidir. Proje temel olarak Service Discovery, API Gateway ve **Resilience4j ile Circuit Breaker** desenlerini demonstrasyonunu içerir.

## 🏗 Mimari ve Servisler

Proje 4 ana bileşenden oluşur:

| Servis | Port | Açıklama |
| :--- | :--- | :--- |
| **EurekaService** | `8761` | Service Discovery sunucusu. Tüm servislerin kayıt defteridir. |
| **ApiGateway** | `8082` | Dış dünyaya açılan tek kapı (Entry Point). Spring Cloud Gateway (WebFlux). |
| **ProductService** | `Random` | Ürün yönetim servisi. PostgreSQL veritabanı kullanır. |
| **OrderService** | `8082*` | Sipariş servisi. Resilience4j Circuit Breaker içerir. |

*(Not: Gateway ve OrderService portları yapılandırmanıza göre çakışmamalıdır, bu demoda dış erişim Gateway (8082) üzerinden sağlanmaktadır.)*

## 🛠 Teknolojiler

* **Dil:** Java 21
* **Framework:** Spring Boot 3.5.x, Spring Cloud 2025.0.0
* **Discovery:** Netflix Eureka Client / Server
* **Gateway:** Spring Cloud Gateway (WebFlux - Reactive)
* **İletişim:** Spring Cloud OpenFeign
* **Resilience:** Spring Cloud Circuit Breaker (Resilience4j)
* **Veritabanı:** PostgreSQL, Spring Data JPA
* **Araçlar:** Lombok, Maven

---

## ⚙️ Kurulum ve Ön Hazırlık

Projeyi çalıştırmadan önce PostgreSQL üzerinde gerekli veritabanlarını oluşturmalısınız.

### 1. Veritabanı Ayarları
* **Host:** `localhost:5432`
* **Kullanıcı:** `postgres`
* **Şifre:** `1905`

