// lib/presentation/providers/listen_together_provider.dart
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/models/listen_together_model.dart';
import '../../data/services/listen_together_service.dart';

final listenTogetherServiceProvider = Provider<ListenTogetherService>((ref) {
  final service = ListenTogetherService();
  ref.onDispose(service.dispose);
  return service;
});

final currentRoomProvider = StreamProvider<ListenTogetherRoom?>((ref) {
  final service = ref.watch(listenTogetherServiceProvider);
  return service.listenToRoomUpdates();
});

final roomMessagesProvider = StreamProvider<List<RoomMessage>>((ref) {
  final service = ref.watch(listenTogetherServiceProvider);
  return service.streamMessages();
});

final isInRoomProvider = Provider<bool>((ref) {
  final service = ref.watch(listenTogetherServiceProvider);
  return service.currentRoomId != null;
});

final isHostProvider = Provider<bool>((ref) {
  final room = ref.watch(currentRoomProvider).value;
  if (room == null) return false;
  final currentUid = FirebaseAuth.instance.currentUser?.uid ?? '';
  return currentUid == room.hostId;
});
