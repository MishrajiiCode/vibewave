// lib/presentation/screens/listen_together/listen_together_screen.dart
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/app_strings.dart';
import '../../../data/models/listen_together_model.dart';
import '../../../data/models/song_model.dart';
import '../../../data/services/listen_together_service.dart';
import '../../providers/listen_together_provider.dart';
import '../../providers/player_provider.dart';

class ListenTogetherScreen extends ConsumerStatefulWidget {
  final String? initialRoomId;

  const ListenTogetherScreen({super.key, this.initialRoomId});

  @override
  ConsumerState<ListenTogetherScreen> createState() =>
      _ListenTogetherScreenState();
}

class _ListenTogetherScreenState
    extends ConsumerState<ListenTogetherScreen> {
  final _roomCodeController = TextEditingController();
  final _messageController = TextEditingController();
  bool _isCreating = false;
  bool _isJoining = false;
  String? _error;
  StreamSubscription<ListenTogetherRoom?>? _roomSub;

  @override
  void initState() {
    super.initState();
    if (widget.initialRoomId != null) {
      _roomCodeController.text = widget.initialRoomId!;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _joinRoom(widget.initialRoomId!);
      });
    }
    // Listen to room updates for guest sync
    _startRoomSync();
  }

  void _startRoomSync() {
    final service = ref.read(listenTogetherServiceProvider);
    _roomSub = service.listenToRoomUpdates().listen((room) {
      if (room == null) return;
      final isHost = ref.read(isHostProvider);
      if (!isHost) {
        _syncAsGuest(room);
      }
    });
  }

  Future<void> _syncAsGuest(ListenTogetherRoom room) async {
    final audioService = ref.read(audioPlayerServiceProvider);
    // Calculate corrected position accounting for elapsed time since last update
    final elapsed = DateTime.now().difference(room.lastUpdatedAt).inMilliseconds;
    final syncPosition = room.positionMs + (room.isPlaying ? elapsed : 0);

    // Seek to synced position
    await audioService.seekToMs(syncPosition.clamp(0, 999999999));

    if (room.isPlaying && !audioService.currentState.isPlaying) {
      await audioService.resume();
    } else if (!room.isPlaying && audioService.currentState.isPlaying) {
      await audioService.pause();
    }
  }

  Future<void> _createRoom() async {
    final currentSong = ref.read(currentSongProvider);
    if (currentSong == null) {
      setState(() => _error = 'Please play a song first to create a room.');
      return;
    }

    setState(() {
      _isCreating = true;
      _error = null;
    });
    try {
      final service = ref.read(listenTogetherServiceProvider);
      final roomId = await service.createRoom(currentSong);
      if (mounted) {
        setState(() => _isCreating = false);
        // Refresh providers
        ref.invalidate(currentRoomProvider);
        ref.invalidate(isInRoomProvider);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isCreating = false;
          _error = 'Failed to create room: $e';
        });
      }
    }
  }

  Future<void> _joinRoom(String code) async {
    final roomId = code.trim().toUpperCase();
    if (roomId.length != 6) {
      setState(() => _error = 'Room code must be 6 characters.');
      return;
    }

    setState(() {
      _isJoining = true;
      _error = null;
    });
    try {
      final service = ref.read(listenTogetherServiceProvider);
      final room = await service.joinRoom(roomId);
      if (room == null) {
        setState(() {
          _isJoining = false;
          _error = 'Room not found. Check the code and try again.';
        });
        return;
      }
      if (mounted) {
        setState(() => _isJoining = false);
        ref.invalidate(currentRoomProvider);
        ref.invalidate(isInRoomProvider);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isJoining = false;
          _error = 'Failed to join room: $e';
        });
      }
    }
  }

  Future<void> _leaveRoom() async {
    final service = ref.read(listenTogetherServiceProvider);
    await service.leaveRoom();
    ref.invalidate(currentRoomProvider);
    ref.invalidate(isInRoomProvider);
    setState(() {});
  }

  @override
  void dispose() {
    _roomCodeController.dispose();
    _messageController.dispose();
    _roomSub?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isInRoom = ref.watch(isInRoomProvider);
    final service = ref.watch(listenTogetherServiceProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.background,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          AppStrings.listenTogether,
          style: TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.w700,
          ),
        ),
        actions: [
          if (isInRoom)
            TextButton(
              onPressed: _leaveRoom,
              child: const Text('Leave', style: TextStyle(color: Colors.red)),
            ),
        ],
      ),
      body: isInRoom && service.currentRoomId != null
          ? _buildRoomView(service.currentRoomId!)
          : _buildJoinCreateView(),
    );
  }

  Widget _buildJoinCreateView() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header illustration
          Container(
            height: 160,
            margin: const EdgeInsets.only(bottom: 32),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  AppColors.primary.withOpacity(0.3),
                  AppColors.secondary.withOpacity(0.1),
                ],
              ),
              borderRadius: BorderRadius.circular(24),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.people_alt_rounded,
                    color: AppColors.primary, size: 60),
                const SizedBox(height: 12),
                const Text(
                  AppStrings.listenTogether,
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  AppStrings.listenTogetherDesc,
                  style: const TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 13,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),

          if (_error != null)
            Container(
              margin: const EdgeInsets.only(bottom: 16),
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.red.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.red.withOpacity(0.3)),
              ),
              child: Text(
                _error!,
                style: const TextStyle(color: Colors.red, fontSize: 13),
              ),
            ),

          // Create Room
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: AppColors.primary.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Icon(Icons.add_circle_outline_rounded,
                          color: AppColors.primary, size: 22),
                    ),
                    const SizedBox(width: 12),
                    const Text(
                      'Create a Room',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                const Text(
                  'Start listening with friends. Share your room code for others to join.',
                  style: TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 13,
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: _isCreating ? null : _createRoom,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.primary,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12)),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                    ),
                    child: _isCreating
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white),
                          )
                        : const Text(
                            'Create Room',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 16),

          // Join Room
          _Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: AppColors.secondary.withOpacity(0.15),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Icon(Icons.login_rounded,
                          color: AppColors.secondary, size: 22),
                    ),
                    const SizedBox(width: 12),
                    const Text(
                      'Join a Room',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _roomCodeController,
                  textCapitalization: TextCapitalization.characters,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    letterSpacing: 4,
                    fontWeight: FontWeight.w700,
                  ),
                  decoration: InputDecoration(
                    hintText: 'ABC123',
                    hintStyle: TextStyle(
                      color: AppColors.textTertiary.withOpacity(0.5),
                      letterSpacing: 4,
                    ),
                    filled: true,
                    fillColor: AppColors.surfaceVariant,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide.none,
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(
                          color: AppColors.secondary, width: 1.5),
                    ),
                    counterText: '',
                  ),
                  maxLength: 6,
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[A-Za-z0-9]')),
                  ],
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: _isJoining
                        ? null
                        : () => _joinRoom(_roomCodeController.text),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.secondary,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12)),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                    ),
                    child: _isJoining
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white),
                          )
                        : const Text(
                            'Join Room',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRoomView(String roomId) {
    final roomAsync = ref.watch(currentRoomProvider);
    final messagesAsync = ref.watch(roomMessagesProvider);
    final isHost = ref.watch(isHostProvider);
    final audioService = ref.read(audioPlayerServiceProvider);

    return roomAsync.when(
      data: (room) {
        if (room == null) {
          return const Center(
            child: Text('Room closed', style: TextStyle(color: Colors.white)),
          );
        }
        return Column(
          children: [
            // Room info header
            Container(
              margin: const EdgeInsets.all(16),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    AppColors.primary.withOpacity(0.2),
                    AppColors.surface,
                  ],
                ),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: AppColors.glassBorder),
              ),
              child: Column(
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: AppColors.primary.withOpacity(0.2),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.radio_button_checked,
                                color: Colors.green, size: 10),
                            const SizedBox(width: 6),
                            Text(
                              'Room: $roomId',
                              style: const TextStyle(
                                color: AppColors.primary,
                                fontSize: 14,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 2,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const Spacer(),
                      IconButton(
                        icon: const Icon(Icons.copy_rounded,
                            color: AppColors.textSecondary, size: 20),
                        onPressed: () {
                          Clipboard.setData(ClipboardData(text: roomId));
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                                content: Text('Room code copied!')),
                          );
                        },
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      const Icon(Icons.music_note_rounded,
                          color: AppColors.primary, size: 16),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          '${room.songTitle} — ${room.songArtist}',
                          style: const TextStyle(
                              color: Colors.white, fontSize: 14),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      const Icon(Icons.people_rounded,
                          color: AppColors.textSecondary, size: 14),
                      const SizedBox(width: 6),
                      Text(
                        '${room.participants.length} listener${room.participants.length == 1 ? '' : 's'}',
                        style: const TextStyle(
                            color: AppColors.textSecondary, fontSize: 13),
                      ),
                      const SizedBox(width: 16),
                      Icon(
                        room.isPlaying
                            ? Icons.play_circle_rounded
                            : Icons.pause_circle_rounded,
                        color: room.isPlaying ? Colors.green : Colors.orange,
                        size: 16,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        room.isPlaying ? 'Playing' : 'Paused',
                        style: TextStyle(
                          color: room.isPlaying ? Colors.green : Colors.orange,
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                  // Host controls
                  if (isHost) ...[
                    const Divider(color: AppColors.glassBorder, height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.admin_panel_settings_rounded,
                            color: AppColors.primary, size: 14),
                        const SizedBox(width: 6),
                        const Text(
                          'You are the host',
                          style: TextStyle(
                              color: AppColors.primary,
                              fontSize: 12,
                              fontWeight: FontWeight.w600),
                        ),
                        const Spacer(),
                        // Sync button
                        TextButton.icon(
                          icon: const Icon(Icons.sync_rounded,
                              color: AppColors.primary, size: 16),
                          label: const Text('Sync All',
                              style: TextStyle(color: AppColors.primary)),
                          onPressed: () {
                            final state = audioService.currentState;
                            final service = ref.read(listenTogetherServiceProvider);
                            service.sendPlaybackUpdate(
                              isPlaying: state.isPlaying,
                              positionMs: state.position.inMilliseconds,
                            );
                          },
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),

            // Chat
            Expanded(
              child: messagesAsync.when(
                data: (messages) => messages.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.chat_bubble_outline_rounded,
                                color: AppColors.textTertiary, size: 40),
                            const SizedBox(height: 8),
                            const Text('No messages yet',
                                style: TextStyle(color: AppColors.textTertiary)),
                          ],
                        ),
                      )
                    : ListView.builder(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        itemCount: messages.length,
                        itemBuilder: (_, i) {
                          final msg = messages[i];
                          return _ChatBubble(message: msg);
                        },
                      ),
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (_, __) => const SizedBox(),
              ),
            ),

            // Message input
            Container(
              padding: const EdgeInsets.all(12),
              decoration: const BoxDecoration(
                color: AppColors.surface,
                border: Border(
                  top: BorderSide(color: AppColors.glassBorder),
                ),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _messageController,
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        hintText: 'Send a message...',
                        hintStyle:
                            const TextStyle(color: AppColors.textTertiary),
                        filled: true,
                        fillColor: AppColors.surfaceVariant,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(20),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: const EdgeInsets.symmetric(
                            horizontal: 16, vertical: 10),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  GestureDetector(
                    onTap: () async {
                      final text = _messageController.text.trim();
                      if (text.isEmpty) return;
                      _messageController.clear();
                      final service =
                          ref.read(listenTogetherServiceProvider);
                      await service.sendMessage(text);
                    },
                    child: Container(
                      width: 44,
                      height: 44,
                      decoration: BoxDecoration(
                        color: AppColors.primary,
                        borderRadius: BorderRadius.circular(22),
                      ),
                      child: const Icon(Icons.send_rounded,
                          color: Colors.white, size: 20),
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (_, __) => Center(
        child: Text('Error loading room',
            style: const TextStyle(color: Colors.white)),
      ),
    );
  }
}

class _Card extends StatelessWidget {
  final Widget child;
  const _Card({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.glassBorder),
      ),
      child: child,
    );
  }
}

class _ChatBubble extends StatelessWidget {
  final RoomMessage message;
  const _ChatBubble({required this.message});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          CircleAvatar(
            radius: 14,
            backgroundColor: AppColors.primary.withOpacity(0.3),
            child: Text(
              message.displayName.isNotEmpty
                  ? message.displayName[0].toUpperCase()
                  : '?',
              style: const TextStyle(
                  color: AppColors.primary,
                  fontSize: 12,
                  fontWeight: FontWeight.w700),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  message.displayName,
                  style: const TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: 2),
                Container(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceVariant,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    message.text,
                    style: const TextStyle(
                        color: Colors.white, fontSize: 14),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
