# IOT_LAB7_20212607

## Instrucciones para Pruebas

### 📱 Roles de Usuario

La aplicación maneja dos roles distintos:
- 🚌 **Rol Empresa**: Gestión de líneas de bus y visualización de ingresos
- 👤 **Rol Operativo**: Usuario regular que puede comprar tickets y suscripciones

### 🔑 Acceso como Empresa

Para facilitar las pruebas, se ha configurado un correo específico que automáticamente tendrá rol de empresa:

```
Email: ampueromario@hotmail.com
```

**Importante**: Al registrarse con este correo específico:
- El usuario será creado automáticamente con rol de empresa
- Todas las líneas de bus existentes serán asignadas a este usuario
- Podrá gestionar y visualizar todas las líneas desde el panel de empresa

### 🎫 Códigos QR de Prueba

Para su comodidad, se proporcionan los QRs de las líneas actuales:
- [QR Línea IO37B](QRs/IO37B.jpg)
- [QR Línea 8104](QRs/8104.jpg)
- [QR Línea IM04](QRs/IM04.jpg)
- [QR Línea IM11](QRs/IM11.jpg)

**Nota**: También puede obtener los códigos QR de cada línea directamente en la aplicación:
- 🏢 **Vista Empresa**: En el detalle de cada línea
- 🎫 **Vista Usuario**: Al ver los detalles de cualquier línea

### 💰 Saldo Inicial

- Los usuarios operativos comienzan con S/ 50.00 de saldo
- Las empresas no manejan saldo

### 🔄 Flujo de Pruebas Recomendado

1. Registrarse con el correo ampueromario@hotmail.com
2. Verificar que todas las líneas aparezcan en el panel de empresa
3. Revisar los QRs de las líneas (puede usar los proporcionados o generarlos en la app)
4. Registrar otro usuario con cualquier otro correo (será usuario operativo)
5. Probar el escaneo de QRs para entrada/salida de buses
6. Verificar el funcionamiento de suscripciones
7. Comprobar el sistema de cashback

### 📊 Sistema de Cashback

- 20% si el viaje dura menos de 15 minutos
- 5% si el viaje dura más de 15 minutos

### 🌟 Funcionalidades Principales

#### Rol Empresa
- Dashboard con ingresos mensuales
- Lista de líneas asignadas
- Gestión de imágenes por línea
- Generación de QRs

#### Rol Operativo
- Escaneo de QRs para entrada/salida
- Sistema de suscripciones
- Historial de viajes
- Cashback automático

### ⚠️ Notas Importantes

- Para un testing completo, se recomienda probar con ambos roles simultáneamente
- Los QRs proporcionados son equivalentes a los generados en la aplicación
- El sistema está preparado para manejar múltiples usuarios y transacciones simultáneas

Por favor, ante cualquier consulta o problema durante las pruebas, no dude en contactarme (a20212607@pucp.edu.pe).
