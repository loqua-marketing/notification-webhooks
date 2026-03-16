# 📱 Notification WEBHOOKS

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![License](https://img.shields.io/badge/license-GPLv3-green.svg)
![Android](https://img.shields.io/badge/Android-5.0%2B-brightgreen)

**Notification WEBHOOKS** é uma aplicação Android de código aberto que permite encaminhar notificações do sistema para endpoints HTTP (webhooks) em tempo real. Automatize tarefas, integre com serviços externos e tome controlo das suas notificações.

---

## ✨ Funcionalidades

### 📥 Captura de Notificações
- Monitorização em tempo real de todas as notificações do sistema
- Extração inteligente de título e conteúdo
- Filtros visuais por status (Recebidas, Pendentes, Automáticas, Ocultas, etc.)

### ⚡ Regras Avançadas
- Criação de regras baseadas em:
  - Aplicação de origem
  - Conteúdo do título
  - Conteúdo do texto
  - Horário específico
  - Dias da semana
  - Tipo de notificação (silenciosa/alertante)
  - Presença de imagem

### 🌐 Gestão de Webhooks
- Múltiplos webhooks guardados
- Webhook fixo "Novo" com http://localhost/
- Teste de webhooks com requisição HEAD
- Suporte para payload personalizado

### 🔐 Segurança
- **HMAC-SHA256**: Assinatura de requests
- **Basic Auth**: Autenticação simples
- **Bearer Token**: Tokens JWT e similares
- **API Key**: Headers personalizados de autenticação

### 📋 Headers Avançados
- Headers personalizados em formato JSON
- Templates pré-definidos (n8n, Zapier, IFTTT, Home Assistant)
- Variáveis dinâmicas:
  - `{{notification.id}}`
  - `{{notification.title}}`
  - `{{notification.text}}`
  - `{{notification.package}}`
  - `{{rule.name}}`

### ⚙️ Configurações Avançadas
- Timeout configurável
- Número de tentativas (retries)
- Payload customizado
- Aguardar resposta do servidor (modo síncrono)

### 💾 Backup & Restore
- Exportar regras para ficheiro JSON
- Importar regras de backup
- Partilhado entre todas as regras

### 🌍 Internacionalização
- Português (PT)
- Inglês (EN)
- Fácil de adicionar novos idiomas

---

## 📸 Capturas de Ecrã

## 📸 Capturas de Ecrã

| Lista de Notificações | Menu de Contexto | Criação de Regras |
|----------------------|-------------------|-------------------|
| ![Notificações](screenshots/notificacoes.png) | ![Menu](screenshots/notificacao-menu.png) | ![Regras](screenshots/regras.png) |

| Passo 1 - App | Passo 2 - Condições | Passo 3 - Ação |
|---------------|---------------------|----------------|
| ![Regras1](screenshots/regras1.png) | ![Regras2](screenshots/regras2.png) | ![Regras](screenshots/regras.png) |

---

## 🚀 Casos de Uso Profissionais

### 💼 **Operações de Servidor (SysAdmin/DevOps)**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Monitorização de servidores** | Recebe alertas de falhas de disco, CPU, memória diretamente no Telegram/Slack via webhook |
| **Backups automatizados** | Notificações de conclusão/erro de backups enviadas para sistema de logging central |
| **SSL Certificate expiry** | Alertas de certificados a expirar encaminhados para equipa de segurança |
| **Deployments** | Confirmações de deploy bem-sucedido enviadas para dashboard de CI/CD |
| **Logs críticos** | Notificações de erros críticos em produção enviadas para sistema de tickets (Jira, ServiceNow) |

### 📈 **Marketing Digital**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Leads em tempo real** | Notificações de novos leads do site/CRM enviadas diretamente para equipa comercial no Slack |
| **Menções nas redes sociais** | Alertas de menções da marca encaminhados para dashboard de social listening |
| **Campanhas de email** | Confirmações de abertura de campanhas enviadas para equipa de marketing |
| **Alertas de concorrência** | Monitorização de atividades da concorrência (preços, promoções) com alertas imediatos |
| **Análise de sentimentos** | Notificações de reviews negativas encaminhadas para equipa de atendimento |

### 🏠 **Home Automation**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Segurança residencial** | Notificações de campainha/movimento enviadas para Home Assistant com trigger em luzes |
| **Irrigação inteligente** | Alertas de humidade do solo para rega automática via webhook |
| **Climatização** | Notificações de temperatura excessiva para ativar ar condicionado |
| **Eletrodomésticos** | Fim de ciclo da máquina de lavar notificado no telemóvel |
| **Energia** | Alertas de consumo elevado para otimizar gastos |

### 📱 **Redes Sociais**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Agregador de notificações** | Centraliza notificações de Instagram, Facebook, LinkedIn, Twitter num único canal (Discord/Telegram) |
| **Publicação automática** | Quando recebe notificação de novo post, dispara workflow para publicar cross-platform |
| **Análise de engagement** | Alertas de novos likes/comentários enviados para dashboard de analytics |
| **Moderação de conteúdo** | Notificações de comentários problemáticos para revisão imediata |
| **Influencer marketing** | Alertas de menções de campanhas para reporting em tempo real |

### 🏦 **Finanças e Bancos**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Alertas de transações** | Notificações de movimentos bancários encaminhadas para sistema de contabilidade |
| **Fraude** | Alertas de transações suspeitas para equipa de segurança |
| **Câmbios** | Notificações de variação cambial para trading |
| **Pagamentos** | Confirmações de pagamento de clientes enviadas para ERP |
| **Investimentos** | Alertas de ações a atingir determinados valores |

### 🏥 **Saúde e Bem-estar**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Telemedicina** | Notificações de novos pedidos de consulta para sistema de agendamento |
| **Medicação** | Alertas de toma de medicamentos para familiares/cuidadores |
| **Monitorização remota** | Dados de dispositivos médicos (tensão, glicemia) enviados para médico |
| **Ginásio** | Notificações de treinos concluídos para app de tracking |
| **Sono** | Alertas de padrões de sono irregulares |

### 📊 **Logística e Supply Chain**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Rastreamento de encomendas** | Atualizações de status de entrega para sistema de tracking |
| **Gestão de stocks** | Alertas de rutura de stock para reposição automática |
| **Entregas** | Notificações de atrasos para equipa de customer service |
| **Frota** | Alertas de manutenção preventiva de veículos |
| **Armazém** | Notificações de receção de mercadoria para inventário |

### 🎓 **Educação**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Plataformas de aprendizagem** | Notificações de novos cursos/módulos para alunos |
| **Avaliações** | Alertas de notas publicadas para sistema de gestão escolar |
| **Presenças** | Registos de presença em aulas enviados para secretaria |
| **Eventos académicos** | Lembretes de prazos de inscrição para comunidade |
| **Biblioteca** | Notificações de empréstimos a expirar |

### 🏨 **Hotelaria e Turismo**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Reservas** | Novas reservas enviadas para sistema de gestão hoteleira |
| **Check-in/out** | Alertas para equipa de limpeza e manutenção |
| **Feedback** | Reviews de hóspedes encaminhadas para equipa de qualidade |
| **Eventos** | Notificações de conferências para organização |
| **Restauração** | Pedidos de room service para cozinha |

### 🎮 **Gaming**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **Eventos de jogo** | Alertas de raids/eventos especiais para guildas no Discord |
| **Conquistas** | Notificações de troféus para streaming/recording |
| **Amigos online** | Alertas quando amigos entram no jogo |
| **Torneios** | Inscrições e resultados enviados para organização |
| **Atualizações** | Notificações de patches e manutenção |

### 🛠️ **Desenvolvimento de Software**

| Cenário | Como o Notification WEBHOOKS ajuda |
|---------|-----------------------------------|
| **CI/CD Pipeline** | Notificações de builds falhadas/sucesso para equipa no Slack |
| **Code reviews** | Alertas de PRs abertos para revisão |
| **Issues** | Novos bugs reportados encaminhados para gestão de projetos (Jira, GitHub) |
| **Testes** | Resultados de testes automatizados enviados para QA |
| **Deploys** | Confirmações de deploy em produção para dashboard |
| **Logs de erro** | Exceções em produção enviadas para sistema de monitoring (Sentry, Datadog) |
| **Métricas** | Alertas de performance (latência, throughput) para equipa de SRE |

❤️ Créditos

Desenvolvido por Nuno Monteiro (loqua.marketing) com o apoio do DeepSeek como parceiro de código.
Se gostas do projeto, considera fazer um donativo:


📞 Contacto

    GitHub: @loqua-marketing
    Issues: Repositório de issues
    E-mail: Relacionado com a app (loqua.marketing@gmail.comm) Assuntos comerciais: info@loqua.marketing
    Website: https://loqua.marketing
