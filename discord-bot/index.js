import 'dotenv/config';
import {
  Client,
  GatewayIntentBits,
  EmbedBuilder,
  Partials,
  SlashCommandBuilder,
  REST,
  Routes,
  Colors,
  ActivityType,
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  AttachmentBuilder,
  PermissionsBitField
} from 'discord.js';
import { createClient } from '@supabase/supabase-js';
import OpenAI from 'openai';
import Groq from 'groq-sdk';

// ============================================================================
// 1. ENVIRONMENT & VALIDATION
// ============================================================================
const REQUIRED_ENV = [
  'DISCORD_TOKEN',
  'CLIENT_ID',
  'CHANNEL_ID',
  'SUPABASE_URL',
  'SUPABASE_KEY',
  'GROQ_API_KEY'
];

// Optional: OPENAI_API_KEY (graceful degradation if missing)
const MISSING_ENV = REQUIRED_ENV.filter((key) => !process.env[key]);

if (MISSING_ENV.length > 0) {
  console.error(`❌ FATAL: Missing Environment Variables: ${MISSING_ENV.join(', ')}`);
  process.exit(1);
}

// Config
const CONFIG = {
  DiscordToken: process.env.DISCORD_TOKEN,
  ClientId: process.env.CLIENT_ID,
  ChannelId: process.env.CHANNEL_ID,
  SupabaseUrl: process.env.SUPABASE_URL,
  SupabaseKey: process.env.SUPABASE_KEY,
  OpenAiKey: process.env.OPENAI_API_KEY,
  GroqKey: process.env.GROQ_API_KEY,
  GroqModel: process.env.GROQ_MODEL || 'llama-3.3-70b-versatile'
};

// ============================================================================
// 2. INITIALIZATION
// ============================================================================
const client = new Client({
  intents: [GatewayIntentBits.Guilds],
  partials: [Partials.Channel],
});

const supabase = createClient(CONFIG.SupabaseUrl, CONFIG.SupabaseKey);
const groq = new Groq({ apiKey: CONFIG.GroqKey });

// Initialize OpenAI conditionally
let openai = null;
const isOpenAiKeyValid = CONFIG.OpenAiKey &&
  !CONFIG.OpenAiKey.includes('your_openai_api_key_here') &&
  CONFIG.OpenAiKey.length > 10;

if (isOpenAiKeyValid) {
  openai = new OpenAI({ apiKey: CONFIG.OpenAiKey });
} else {
  console.warn('⚠️  WARN: OPENAI_API_KEY is missing or invalid. RAG functionality will be disabled.');
}

const SYSTEM_PROMPT =
  `You are GeoOps, the Site Reliability Engineer for GeoRacing. You value EFFICIENCY, SAFETY, and OFFLINE-FIRST architecture. You are NOT a marketing assistant. Answer technical questions based strictly on the provided Context. If the context is missing, rely on the principle that 'Safety > Wow' and 'Battery is Survival'. Always answer in Spanish.`;

// ============================================================================
// 3. COMMANDS DEFINITION
// ============================================================================
const COMMANDS = [
  new SlashCommandBuilder()
    .setName('duda')
    .setDescription('Consulta técnica al GeoOps (RAG)')
    .addStringOption(opt =>
      opt.setName('question')
        .setDescription('Pregunta técnica')
        .setRequired(true)
    ),
  new SlashCommandBuilder()
    .setName('estado')
    .setDescription('Monitor de estado del sistema (Ideas)'),
  new SlashCommandBuilder()
    .setName('ping')
    .setDescription('Verificar latencia del sistema'),
  new SlashCommandBuilder()
    .setName('aprender')
    .setDescription('Ingestar documentación (Texto o Archivo .md/.txt)')
    .addStringOption(opt =>
      opt.setName('texto')
        .setDescription('Contenido de texto directo')
        .setRequired(false)
    )
    .addAttachmentOption(opt =>
      opt.setName('archivo')
        .setDescription('Archivo de documentación (.txt, .md)')
        .setRequired(false)
    ),
];

// ============================================================================
// 4. MAIN LOGIC
// ============================================================================

client.once('ready', async () => {
  console.log(`\n🟢 SYSTEM ONLINE: ${client.user.tag}`);
  client.user.setPresence({
    activities: [{ name: 'Localizándome en el circuito 🏎️', type: ActivityType.Playing }],
    status: 'online'
  });
  console.log(`🔌 Mode: KEYWORD SEARCH (No OpenAI needed)`);

  // A. Auto-Deploy Commands
  await deployCommands();

  // B. Start Watchdogs
  await verifyChannelAccess();
  startRealtimeWatchdog();

  console.log(`�️  GeoOps Watchdog Active on Public.Ideas\n`);
});

client.on('interactionCreate', async (interaction) => {
  if (!interaction.isChatInputCommand()) return;

  const { commandName } = interaction;

  try {
    if (commandName === 'duda') await handleDuda(interaction);
    else if (commandName === 'estado') await handleEstado(interaction);
    else if (commandName === 'ping') await handlePing(interaction);
    else if (commandName === 'aprender') await handleAprender(interaction);
    else if (commandName === 'incidente') await handleIncidente(interaction);
    else if (commandName === 'emitir') await handleEmitir(interaction);
    else if (commandName === 'equipo') await handleEquipo(interaction);
    else if (commandName === 'purgar') await handlePurgar(interaction);
    else if (commandName === 'perfil') await handlePerfil(interaction);
  } catch (error) {
    console.error(`💥 CMD ERROR [${commandName}]:`, error);
    await sendErrorEmbed(interaction, 'Command Execution Failure', error.message);
  }
});

// Button Handler (Outside the chatInput check)
client.on('interactionCreate', async (interaction) => {
  if (!interaction.isButton()) return;

  if (interaction.customId.startsWith('role_')) {
    await handleRoleButton(interaction);
  }
});

async function handleRoleButton(interaction) {
  await interaction.deferReply({ ephemeral: true });
  const roleMap = {
    'role_telemetry': 'Equipo Telemetría',
    'role_track': 'Equipo Pista',
    'role_logistics': 'Equipo Logística'
  };

  const roleName = roleMap[interaction.customId];
  if (!roleName) return;

  const guild = interaction.guild;
  let role = guild.roles.cache.find(r => r.name === roleName);

  // Auto-create role if missing
  if (!role) {
    try {
      role = await guild.roles.create({
        name: roleName,
        color: Colors.Blue,
        reason: 'GeoOps Auto-Role'
      });
    } catch (err) {
      return await interaction.editReply({ content: '❌ Error: No puedo crear el rol. Verifica mis permisos.' });
    }
  }

  const member = interaction.member;
  if (member.roles.cache.has(role.id)) {
    await member.roles.remove(role);
    await interaction.editReply({ content: `➖ Te has quitado el rol **${roleName}**.` });
  } else {
    await member.roles.add(role);
    await interaction.editReply({ content: `➕ Asignado al **${roleName}**.` });
  }
}

// ============================================================================
// 5. FUNCTIONALITIES
// ============================================================================

/**
 * Auto-registers Slash Commands globally.
 */
async function deployCommands() {
  try {
    console.log('🔄 Syncing Slash Commands...');
    const rest = new REST({ version: '10' }).setToken(CONFIG.DiscordToken);

    await rest.put(
      Routes.applicationCommands(CONFIG.ClientId),
      { body: COMMANDS.map(c => c.toJSON()) }
    );
    console.log('✅ Commands Synced.');
  } catch (error) {
    console.error('❌ Command Sync Failed:', error);
  }
}

/**
 * /duda handler: RAG Pipeline
 */
async function handleDuda(interaction) {
  await interaction.deferReply();
  const question = interaction.options.getString('question');
  let context = "";

  // 1. Retrieval (Text Search via Supabase)
  try {
    context = await fetchContext(question);
  } catch (err) {
    console.error('Supabase Search Error:', err);
    context = "ERROR: Database connection failed.";
  }

  // 2. Generation
  const answer = await askGroq(question, context);

  // 3. Response
  let status = '⚠️ Unknown';
  if (context.includes('ERROR')) status = '❌ Error';
  else if (context) status = '✅ Found';
  else status = '⚠️ No Matches';

  const embed = createIndustrialEmbed('GeoOps Terminal', answer)
    .addFields({ name: 'Knowledge Base', value: status, inline: true });

  await interaction.editReply({ embeds: [embed] });
}

/**
 * /estado handler: System Status
 */
async function handleEstado(interaction) {
  await interaction.deferReply();

  const { data, error } = await supabase.from('ideas').select('status');

  if (error) throw new Error(`Database Query Failed: ${error.message}`);

  const stats = {
    'Acabado': 0,
    'en proceso': 0,
    'por empezar': 0
  };

  data.forEach(row => {
    if (stats[row.status] !== undefined) stats[row.status]++;
    else {
      // Handle unexpected statuses
      stats['Other'] = (stats['Other'] || 0) + 1;
    }
  });

  const total = data.length;

  const embed = createIndustrialEmbed('System Status Report', 'Current project metrics.')
    .addFields(
      { name: '🟢 Sync Complete', value: `${stats['Acabado'] || 0}`, inline: true },
      { name: '🟡 Processing', value: `${stats['en proceso'] || 0}`, inline: true },
      { name: '⚪ Pending', value: `${stats['por empezar'] || 0}`, inline: true }
    )
    .setFooter({ text: `GeoRacing Systems | Total Units: ${total}` });

  await interaction.editReply({ embeds: [embed] });
}

/**
 * /ping handler
 */
async function handlePing(interaction) {
  const sent = await interaction.reply({ content: 'Pinging...', fetchReply: true });
  const latency = sent.createdTimestamp - interaction.createdTimestamp;

  const embed = createIndustrialEmbed('Network Diagnostics', '')
    .addFields(
      { name: 'Latency', value: `${latency}ms`, inline: true },
      { name: 'API Latency', value: `${Math.round(client.ws.ping)}ms`, inline: true }
    );

  await interaction.editReply({ content: null, embeds: [embed] });
}

/**
 * /aprender handler: Ingest Documentation
 */
async function handleAprender(interaction) {
  await interaction.deferReply();

  const textInput = interaction.options.getString('texto');
  const fileAttachment = interaction.options.getAttachment('archivo');

  let contentToIngest = "";
  let sourceName = "Direct Input";

  // 1. Validate Input
  if (!textInput && !fileAttachment) {
    return await interaction.editReply({
      content: '❌ Debes proporcionar al menos una opción: `texto` o `archivo`.'
    });
  }

  // 2. Process File (if exists)
  if (fileAttachment) {
    const validExtensions = ['txt', 'md', 'markdown'];
    const fileExt = fileAttachment.name.split('.').pop().toLowerCase();

    if (!validExtensions.includes(fileExt)) {
      return await interaction.editReply({ content: '❌ Formato no soportado. Usa `.txt` o `.md`.' });
    }

    try {
      const response = await fetch(fileAttachment.url);
      if (!response.ok) throw new Error('Failed to download file');
      const fileText = await response.text();

      contentToIngest += `\n\n--- FILE: ${fileAttachment.name} ---\n${fileText}`;
      sourceName = fileAttachment.name;
    } catch (err) {
      console.error('File Download Error:', err);
      return await interaction.editReply({ content: '❌ Error descargando el archivo adjunto.' });
    }
  }

  // 3. Process Direct Text (if exists)
  if (textInput) {
    contentToIngest += `\n\n--- NOTE ---\n${textInput}`;
  }

  // 4. Save to Database
  const { error } = await supabase
    .from('documents')
    .insert([{ content: contentToIngest.trim() }]);

  if (error) {
    console.error('DB Insert Error:', error.message);
    const errEmbed = createIndustrialEmbed('Ingestion Failed', `Database rejected the payload.\n${error.message}`)
      .setColor(Colors.Red);
    return await interaction.editReply({ embeds: [errEmbed] });
  }

  // 5. Success
  const embed = createIndustrialEmbed('Knowledge Ingested', `Successfully added to database.`)
    .addFields(
      { name: 'Source', value: sourceName, inline: true },
      { name: 'Bytes', value: `${contentToIngest.length}`, inline: true }
    )
    .setColor(Colors.Green);

  await interaction.editReply({ embeds: [embed] });
}

// ============================================================================
// 6. HELPERS & UTILS
// ============================================================================

/**
 * Search Supabase using Multi-Stage Fallback strategy
 */
async function fetchContext(query) {
  console.log(`🔎 Searching Knowledge Base for: "${query}"...`);

  // Strategy 1: 'plain' Text Search (Split by spaces)
  // This is better than 'websearch' for simple queries in Supabase
  const searchTerms = query.replace(/[^\w\s]/gi, '').split(' ').filter(w => w.length > 3).join(' | ');

  if (searchTerms) {
    const { data: textData } = await supabase
      .from('documents')
      .select('content')
      .textSearch('content', searchTerms, { config: 'spanish' })
      .limit(3);

    if (textData && textData.length > 0) {
      console.log(`✅ Found matches via TextSearch ('${searchTerms}')`);
      return textData.map(d => d.content).join('\n---\n').slice(0, 4000);
    }
  }

  // Strategy 2: "Smart" Keyword Extraction + ILIKE
  // Extract capitalized words (e.g., "EcoMeter", "GeoRacing") or long words
  const keywords = query.match(/\b[A-Za-zñÑáéíóú]{4,}\b/g) || [];

  if (keywords.length > 0) {
    console.log(`⚠️ TextSearch failed. Trying keyword fallback: [${keywords.join(', ')}]`);

    // Try matching ANY of the keywords
    for (const word of keywords) {
      const { data: simpleData } = await supabase
        .from('documents')
        .select('content')
        .ilike('content', `%${word}%`)
        .limit(3);

      if (simpleData && simpleData.length > 0) {
        console.log(`✅ Found matches via Fallback Keyword: "${word}"`);
        return simpleData.map(d => d.content).join('\n---\n').slice(0, 4000);
      }
    }
  }

  console.log('❌ No results found in DB after all strategies.');
  return "";
}

async function askGroq(question, context) {
  try {
    const chatCompletion = await groq.chat.completions.create({
      messages: [
        { role: 'system', content: SYSTEM_PROMPT },
        { role: 'user', content: `CONTEXT:\n${context}\n\nQUESTION:\n${question}` }
      ],
      model: CONFIG.GroqModel,
      temperature: 0.1, // Low temp for "Technical/Safety" precision
      max_tokens: 1024
    });

    return chatCompletion.choices[0]?.message?.content || "Err: No response generated.";
  } catch (error) {
    console.error('Groq Inference Error:', error);
    return "CRITICAL: Inference Engine Failure.";
  }
}

/**
 * Creates a standardized 'Industrial' style embed.
 */
function createIndustrialEmbed(title, description) {
  return new EmbedBuilder()
    .setColor(Colors.Grey) // Industrial Grey
    .setTitle(`🏭 ${title}`)
    .setDescription(description || null)
    .setTimestamp()
    .setFooter({ text: 'GeoRacing Systems | v1.0.0' });
}

async function sendErrorEmbed(interaction, title, errorMsg) {
  const embed = new EmbedBuilder()
    .setColor(Colors.Red)
    .setTitle(`⚠️ ${title}`)
    .setDescription(`\`\`\`${errorMsg}\`\`\``)
    .setTimestamp();

  if (interaction.deferred || interaction.replied) {
    await interaction.editReply({ embeds: [embed] });
  } else {
    await interaction.reply({ embeds: [embed], ephemeral: true });
  }
}

async function verifyChannelAccess() {
  try {
    const channel = await client.channels.fetch(CONFIG.ChannelId);
    if (!channel) throw new Error("Channel not found");
    // Optional: Send boot message
    // await channel.send({ embeds: [createIndustrialEmbed('System Boot', 'GeoOps initialized successfully.')] });
  } catch (error) {
    console.error(`❌ Channel Verification Failed (ID: ${CONFIG.ChannelId}):`, error.message);
  }
}

/**
 * /incidente handler: Report an Issue
 */
async function handleIncidente(interaction) {
  await interaction.deferReply({ ephemeral: true });
  const title = interaction.options.getString('titulo');
  const severity = interaction.options.getString('severidad');

  // 1. Create DB Ticket
  const { data, error } = await supabase
    .from('incidents')
    .insert([{
      user_id: interaction.user.id,
      user_tag: interaction.user.tag,
      title: title,
      severity: severity,
      status: 'ABIERTO'
    }])
    .select()
    .single();

  if (error) {
    return await interaction.editReply({ content: `❌ Error creando ticket: ${error.message}` });
  }

  // 2. Create Public Thread
  try {
    const channel = await interaction.channel;
    const thread = await channel.threads.create({
      name: `[${severity}] ${title}`,
      autoArchiveDuration: 1440,
      reason: 'GeoOps Incident Report',
    });

    await thread.send(`⚠️ **INCIDENTE #${data.id} REPORTADO POR ${interaction.user}**\nSeveridad: ${severity}\n\nCc: @here`);

    // Update DB with thread ID
    await supabase.from('incidents').update({ thread_id: thread.id }).eq('id', data.id);

    await interaction.editReply({ content: `✅ Incidente #${data.id} registrado. Hilo creado: ${thread}` });
  } catch (err) {
    await interaction.editReply({ content: `✅ Ticket creado en DB (#${data.id}), pero falló la creación del hilo: ${err.message}` });
  }
}

/**
 * /emitir handler: Broadcast Announcement
 */
async function handleEmitir(interaction) {
  // Security Check
  if (!interaction.member.permissions.has(PermissionsBitField.Flags.Administrator)) {
    return await interaction.reply({ content: '🚫 Acceso Denegado: Solo Administradores.', ephemeral: true });
  }

  await interaction.deferReply({ ephemeral: true });

  const title = interaction.options.getString('titulo');
  const message = interaction.options.getString('mensaje');
  const image = interaction.options.getAttachment('imagen');
  const mention = interaction.options.getString('mencion') === 'yes';

  const embed = createIndustrialEmbed(`📢 ${title}`, message)
    .setColor(Colors.Blue)
    .setAuthor({ name: `Emitido por: ${interaction.user.tag}`, iconURL: interaction.user.displayAvatarURL() });

  if (image) embed.setImage(image.url);

  const channel = interaction.channel;
  await channel.send({
    content: mention ? '@everyone' : null,
    embeds: [embed]
  });

  await interaction.editReply({ content: '✅ Comunicado emitido correctamente.' });
}

/**
 * /purgar handler: Bulk Delete
 */
async function handlePurgar(interaction) {
  if (!interaction.member.permissions.has(PermissionsBitField.Flags.ManageMessages)) {
    return await interaction.reply({ content: '🚫 Requiere permiso "Gestionar Mensajes".', ephemeral: true });
  }

  const amount = interaction.options.getInteger('cantidad');
  if (amount > 100 || amount < 1) {
    return await interaction.reply({ content: '❌ Cantidad debe ser entre 1 y 100.', ephemeral: true });
  }

  await interaction.deferReply({ ephemeral: true });

  try {
    const deleted = await interaction.channel.bulkDelete(amount, true);
    await interaction.editReply({ content: `🧹 Se han eliminado ${deleted.size} mensajes.` });
  } catch (err) {
    await interaction.editReply({ content: `❌ Error al purgar: ${err.message}` });
  }
}

/**
 * /perfil handler: User Stats
 */
async function handlePerfil(interaction) {
  await interaction.deferReply();
  const user = interaction.user;

  // Count incidents reported
  const { count, error } = await supabase
    .from('incidents')
    .select('*', { count: 'exact', head: true })
    .eq('user_id', user.id);

  const embed = createIndustrialEmbed(`Ficha de Operario: ${user.username}`, '')
    .setThumbnail(user.displayAvatarURL())
    .addFields(
      { name: '🆔 ID', value: `\`${user.id}\``, inline: true },
      { name: '📅 Ingreso', value: `<t:${Math.floor(interaction.member.joinedTimestamp / 1000)}:R>`, inline: true },
      { name: '🚨 Incidentes Reportados', value: `${count || 0}`, inline: true }
    );

  await interaction.editReply({ embeds: [embed] });
}

/**
 * /equipo handler: Role Selection Panel
 */
async function handleEquipo(interaction) {
  const row = new ActionRowBuilder().addComponents(
    new ButtonBuilder().setCustomId('role_telemetry').setLabel('📡 Telemetría').setStyle(ButtonStyle.Primary),
    new ButtonBuilder().setCustomId('role_track').setLabel('🏁 Pista').setStyle(ButtonStyle.Success),
    new ButtonBuilder().setCustomId('role_logistics').setLabel('📦 Logística').setStyle(ButtonStyle.Secondary)
  );

  const embed = createIndustrialEmbed('Asignación de Equipo', 'Selecciona tu departamento operativo para recibir las alertas correspondientes.')
    .setColor(Colors.Navy);

  await interaction.reply({ embeds: [embed], components: [row] });
}

// ============================================================================
// 7. REALTIME WATCHDOG
// ============================================================================

function startRealtimeWatchdog() {
  supabase
    .channel('public:ideas')
    .on('postgres_changes', { event: 'UPDATE', schema: 'public', table: 'ideas' }, handleDatabaseUpdate)
    .subscribe((status) => {
      console.log(`📡 Realtime Status: ${status}`);
    });
}

async function handleDatabaseUpdate(payload) {
  const { old: oldRecord, new: newRecord } = payload;

  // 1. Debounce/Filter: Ignore if status hasn't changed
  if (oldRecord.status === newRecord.status) return;

  // 2. Visual Coding
  let statusColor = Colors.Grey;
  if (newRecord.status === 'Acabado') statusColor = Colors.Green;
  else if (newRecord.status === 'en proceso') statusColor = Colors.Yellow;

  const embed = new EmbedBuilder()
    .setTitle('🔄 Status Change Detected')
    .setColor(statusColor)
    .addFields(
      { name: 'Item', value: newRecord.title || 'Untitled', inline: false },
      { name: 'Old Status', value: `\`${oldRecord.status}\``, inline: true },
      { name: 'New Status', value: `**${newRecord.status}**`, inline: true }
    )
    .setTimestamp()
    .setFooter({ text: 'GeoRacing Watchdog' });

  try {
    const channel = await client.channels.fetch(CONFIG.ChannelId);
    if (channel) await channel.send({ embeds: [embed] });
  } catch (err) {
    console.error('Failed to send Watchdog Alert:', err);
  }
}

// ============================================================================
// 8. GLOBAL ERROR HANDLING
// ============================================================================
process.on('unhandledRejection', (reason, promise) => {
  console.error('🚨 Unhandled Rejection at:', promise, 'reason:', reason);
});

process.on('uncaughtException', (error) => {
  console.error('🚨 Uncaught Exception:', error);
  // Keep alive strategy: don't exit unless critical
});

// Login
client.login(CONFIG.DiscordToken);
