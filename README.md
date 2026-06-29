# Royal Crown - MineColonies

**Royal Crown - MineColonies** e uma expansao narrativa leve para
[MineColonies](https://www.curseforge.com/minecraft/mc-mods/minecolonies).

O objetivo do mod e fazer a colonia reconhecer o jogador como seu governante. Em MineColonies, o jogador funda,
administra e protege uma colonia, mas os guardas ainda podem tratar o proprio lider como qualquer outro alvo. Royal
Crown muda essa sensacao: a coroa precisa ser conquistada, a colonia testemunha a coroacao, e os guardas passam a
respeitar e defender o rei.

---

## Versao Atual

### 1.1.0

Principais mudancas:

- Adicionada cerimonia de coroacao antes da entrega da coroa.
- Cidadaos e guardas proximos sao chamados para perto da Prefeitura usando pathfinding.
- Participantes olham para o jogador durante a cerimonia.
- Cidadaos pulam de forma aleatoria durante a celebracao.
- A coroa so e entregue no fim da cerimonia, com requisitos revalidados.
- Adicionadas configuracoes da cerimonia no `royalcrown-common.toml`.
- Adicionados comandos de debug para acelerar testes locais.
- `/royalcrown status` agora mostra progresso, aceite das provas, estado da cerimonia e dono da coroa.
- `gradlew` agora esta executavel para builds locais.

---

## Funcionalidades

### Conselheiro Real

O **Conselheiro Real** aparece perto da Prefeitura quando a colonia atinge o ponto inicial da jornada real.

O Conselheiro:

- apresenta o conceito das provas reais;
- permite aceitar a jornada de legitimidade;
- mostra o progresso atual;
- entrega a coroa quando os requisitos forem cumpridos;
- pode ser localizado com comando.

### Provas Reais

Antes de ser coroado, o jogador precisa provar que a colonia tem base suficiente para reconhecer seu titulo.

Requisitos atuais:

- atingir uma quantidade minima de cidadaos;
- concluir defesas perto da colonia;
- retornar ao Conselheiro Real para reivindicar a coroa.

As defesas sao contadas quando o jogador derrota mobs hostis perto da colonia apos aceitar as provas.

### Cerimonia de Coroacao

Ao reivindicar a coroa, o jogador nao recebe o item imediatamente. O Conselheiro inicia uma cerimonia perto da
Prefeitura.

Durante a cerimonia:

- cidadaos e guardas proximos sao chamados para a area;
- participantes tentam se aproximar sem teleportar;
- guardas tendem a ficar mais perto do centro;
- cidadaos olham para o jogador;
- cidadaos pulam aleatoriamente durante a celebracao;
- sons, particulas e fogos marcam a proclamacao;
- a coroa e entregue apenas no fim.

Se a cerimonia estiver desativada na configuracao, a coroa e entregue diretamente.

### Coroa do Rei

A **Coroa do Rei** representa reconhecimento, comando e legitimidade.

Com a coroa equipada:

- guardas/cidadaos do MineColonies nao retaliam contra o jogador coroado;
- guardas proximos podem focar o alvo atacado pelo rei;
- guardas proximos podem defender o rei contra agressores;
- alvos protegidos, como cidadaos, guardas e animais domesticados, nao sao tratados como alvos validos para ordens da
  coroa.

### Status e Localizacao

O mod adiciona comandos para acompanhar a jornada real e localizar o Conselheiro.

Comandos principais:

- `/royalcrown help`: mostra os comandos disponiveis.
- `/royalcrown status`: mostra progresso, estado da cerimonia e dono da coroa.
- `/royalcrown advisor where`: localiza ou destaca o Conselheiro Real.

### Comandos de Debug

Os comandos de debug nao exigem permissao especial no momento. Eles existem para acelerar testes locais durante o
desenvolvimento.

- `/royalcrown debug accept`: marca as provas como aceitas.
- `/royalcrown debug complete_defenses`: completa o requisito de defesas do jogador atual.
- `/royalcrown debug start_coronation`: inicia a cerimonia se os requisitos estiverem completos.
- `/royalcrown debug reset_player`: reinicia o progresso real do jogador atual.
- `/royalcrown debug clear_crown`: limpa o registro global de dono da coroa.
- `/royalcrown debug give_crown`: entrega a coroa diretamente ao jogador atual.
- `/royalcrown debug respawn_advisor`: garante o Conselheiro perto da Prefeitura mais proxima.

Observacao: `complete_defenses` nao falsifica populacao. A populacao continua sendo contada pela colonia.

---

## Itens

### Coroa do Rei

A coroa nao deve ser obtida por craft comum. Ela e a recompensa final da jornada real.

No estado atual, a receita aponta para um item invalido de proposito para impedir craft normal. Se isso causar ruido em
logs, JEI ou datapacks, a abordagem deve ser trocada por um bloqueio mais limpo sem tornar a coroa craftavel.

---

## Requisitos

- **Minecraft:** 1.20.1
- **Loader:** Forge 47+
- **Java:** 17 ou superior
- **MineColonies**
- Dependencias usadas pelo MineColonies no modpack, como Structurize, Domum Ornamentum e BlockUI, quando aplicavel.

---

## Observacoes Tecnicas

- A integracao com MineColonies usa reflexao em alguns pontos para reduzir acoplamento com uma forma especifica da API.
- A contagem de cidadaos tenta usar dados da colonia e cai para contagem de entidades proximas se necessario.
- A cerimonia usa pathfinding e pequenas interferencias temporarias na IA; ela nao teleporta cidadaos.
- O estado da cerimonia fica em memoria e nao deve prender progresso se o jogador sair do mundo.
- O registro global da coroa respeita a configuracao `trial.uniquePerWorld`.
- Guardas sao detectados por heuristica com base em cidadaos do MineColonies carregando espada, machado, arco ou besta.

---

## Configuracao

O mod gera o arquivo `royalcrown-common.toml`.

Principais opcoes:

- `disableGuardRetaliation`: guardas nao revidam contra o jogador coroado.
- `enableWolfMode`: guardas ajudam quando o rei ataca um alvo valido.
- `enableDefendNeutrals`: guardas defendem o rei contra agressores.
- `guardHelpRadius`: raio de ajuda dos guardas.
- `forcedCooldownMultiplier`: multiplicador do cooldown de ataques forcados.
- `forcedDamageMultiplier`: multiplicador do dano de ataques forcados.
- `trial.requiredCitizens`: cidadaos minimos para conquistar a coroa.
- `trial.defensesRequired`: defesas completas necessarias.
- `trial.waveKills`: kills necessarias para contar uma defesa.
- `trial.waveTimeoutTicks`: janela de tempo para agrupar kills em uma defesa.
- `trial.nearRadius`: raio usado para considerar combate perto da colonia.
- `trial.uniquePerWorld`: limita a coroa a um dono global por mundo.
- `trial.allowReclaim`: reserva para permitir reaver a coroa futuramente.
- `coronation.enabled`: ativa ou desativa a cerimonia.
- `coronation.gatherTicks`: duracao da fase de reuniao.
- `coronation.celebrateTicks`: duracao da fase de celebracao.
- `coronation.searchRadius`: raio para buscar participantes.
- `coronation.maxParticipants`: limite de participantes.
- `coronation.jumpChance`: chance de pulo durante a celebracao.

---

## Desenvolvimento

Build local usado neste ambiente:

```sh
JAVA_HOME=/home/hyanferreira/.jdks/ms-17.0.19 PATH=/home/hyanferreira/.jdks/ms-17.0.19/bin:$PATH ./gradlew build
```

O jar gerado fica em:

```text
build/libs/royalcrown-1.20.1-1.1.0.jar
```

---

## Licenca

Este projeto esta licenciado sob **MIT**.

---

## Links

- GitHub: https://github.com/HyanFerreira/royalcrown
- Perfil do autor: https://github.com/HyanFerreira
