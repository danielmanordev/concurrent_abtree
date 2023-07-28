output "ip" {
  value = azurerm_public_ip.public_ip.ip_address
  depends_on = [azurerm_public_ip.public_ip]
}