// lib/presentation/screens/admin/add_song_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_animate/flutter_animate.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../../core/constants/app_colors.dart';
import '../../../data/models/song_model.dart';
import '../../../data/models/category_model.dart';
import '../../../data/services/firebase_music_service.dart';
import '../../../data/services/youtube_service.dart';
import '../../providers/music_provider.dart';

class AddSongScreen extends ConsumerStatefulWidget {
  final String? songId;
  const AddSongScreen({super.key, this.songId});

  @override
  ConsumerState<AddSongScreen> createState() => _AddSongScreenState();
}

class _AddSongScreenState extends ConsumerState<AddSongScreen> {
  final _formKey = GlobalKey<FormState>();
  final _titleCtrl = TextEditingController();
  final _artistCtrl = TextEditingController();
  final _albumCtrl = TextEditingController();
  final _urlCtrl = TextEditingController();
  final _thumbnailCtrl = TextEditingController();
  final _tagsCtrl = TextEditingController();
  final _lyricsCtrl = TextEditingController();

  String _selectedCategory = 'Pop';
  String _selectedLanguage = 'English';
  bool _isFeatured = false;
  bool _isLoading = false;
  bool _isFetchingInfo = false;
  String? _previewThumbnail;
  SongModel? _existingSong;

  @override
  void initState() {
    super.initState();
    if (widget.songId != null) _loadExistingSong();
  }

  Future<void> _loadExistingSong() async {
    // Would load existing song from Firestore
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _artistCtrl.dispose();
    _albumCtrl.dispose();
    _urlCtrl.dispose();
    _thumbnailCtrl.dispose();
    _tagsCtrl.dispose();
    _lyricsCtrl.dispose();
    super.dispose();
  }

  Future<void> _fetchYouTubeInfo() async {
    final url = _urlCtrl.text.trim();
    if (url.isEmpty) return;

    setState(() => _isFetchingInfo = true);
    try {
      final info = await YouTubeService.getVideoInfo(url);
      if (info != null) {
        _titleCtrl.text = info['title'] ?? '';
        _artistCtrl.text = info['author'] ?? '';
        final thumb = info['thumbnailUrl'] ?? '';
        _thumbnailCtrl.text = thumb;
        setState(() => _previewThumbnail = thumb);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('YouTube info fetched!')),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text('Error fetching info: $e'),
            backgroundColor: AppColors.error),
      );
    } finally {
      if (mounted) setState(() => _isFetchingInfo = false);
    }
  }

  Future<void> _saveSong() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isLoading = true);

    try {
      final url = _urlCtrl.text.trim();
      final videoId = SongModel.extractVideoId(url);
      final thumbnail = _thumbnailCtrl.text.trim().isNotEmpty
          ? _thumbnailCtrl.text.trim()
          : SongModel.getThumbnailUrl(videoId);

      final song = SongModel(
        id: _existingSong?.id ?? '',
        title: _titleCtrl.text.trim(),
        artist: _artistCtrl.text.trim(),
        album: _albumCtrl.text.trim(),
        youtubeUrl: url,
        videoId: videoId,
        thumbnailUrl: thumbnail,
        category: _selectedCategory,
        tags: _tagsCtrl.text.split(',').map((t) => t.trim()).where((t) => t.isNotEmpty).toList(),
        language: _selectedLanguage,
        isFeatured: _isFeatured,
        lyrics: _lyricsCtrl.text.trim(),
        createdAt: _existingSong?.createdAt ?? DateTime.now(),
        updatedAt: DateTime.now(),
      );

      if (_existingSong != null) {
        await FirebaseMusicService.updateSong(song);
      } else {
        await FirebaseMusicService.addSong(song);
      }

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(_existingSong != null ? 'Song updated!' : 'Song added!'),
            backgroundColor: AppColors.success,
          ),
        );
        context.go('/admin/dashboard');
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text('Error: $e'), backgroundColor: AppColors.error),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final categories = ref.watch(categoriesProvider);
    final catList = categories.whenOrNull(data: (c) => c) ?? CategoryModel.defaults;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        title: Text(
          widget.songId != null ? 'Edit Song' : 'Add Song',
          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700),
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded, color: Colors.white),
          onPressed: () => context.go('/admin/dashboard'),
        ),
        actions: [
          TextButton(
            onPressed: _isLoading ? null : _saveSong,
            child: _isLoading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                        color: AppColors.primary, strokeWidth: 2),
                  )
                : const Text('Save',
                    style: TextStyle(
                        color: AppColors.primary, fontWeight: FontWeight.w700)),
          ),
        ],
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            // YouTube URL with fetch button
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  child: TextFormField(
                    controller: _urlCtrl,
                    style: const TextStyle(color: Colors.white),
                    validator: (v) {
                      if (v == null || v.isEmpty) return 'Required';
                      if (!v.contains('youtube') && !v.contains('youtu.be')) {
                        return 'Enter a valid YouTube URL';
                      }
                      return null;
                    },
                    decoration: const InputDecoration(
                      labelText: 'YouTube URL *',
                      hintText: 'https://youtube.com/watch?v=...',
                      prefixIcon: Icon(Icons.link_rounded,
                          color: AppColors.textTertiary),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _isFetchingInfo ? null : _fetchYouTubeInfo,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.surfaceVariant,
                    padding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 16),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14)),
                  ),
                  child: _isFetchingInfo
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                              color: AppColors.primary, strokeWidth: 2))
                      : const Icon(Icons.auto_fix_high_rounded,
                          color: AppColors.primary),
                ),
              ],
            ).animate().fadeIn(duration: 400.ms),

            const SizedBox(height: 16),

            // Thumbnail preview
            if (_previewThumbnail != null || _thumbnailCtrl.text.isNotEmpty) ...[
              ClipRRect(
                borderRadius: BorderRadius.circular(14),
                child: CachedNetworkImage(
                  imageUrl: _previewThumbnail ?? _thumbnailCtrl.text,
                  height: 150,
                  width: double.infinity,
                  fit: BoxFit.cover,
                  errorWidget: (_, __, ___) => const SizedBox(),
                ),
              ),
              const SizedBox(height: 16),
            ],

            _buildTextField(
              controller: _titleCtrl,
              label: 'Song Title *',
              icon: Icons.music_note_rounded,
              validator: (v) => v?.isEmpty == true ? 'Required' : null,
            ),
            const SizedBox(height: 12),
            _buildTextField(
              controller: _artistCtrl,
              label: 'Artist Name *',
              icon: Icons.person_outline_rounded,
              validator: (v) => v?.isEmpty == true ? 'Required' : null,
            ),
            const SizedBox(height: 12),
            _buildTextField(
              controller: _albumCtrl,
              label: 'Album (optional)',
              icon: Icons.album_outlined,
            ),
            const SizedBox(height: 12),
            _buildTextField(
              controller: _thumbnailCtrl,
              label: 'Thumbnail URL (auto-filled)',
              icon: Icons.image_outlined,
              onChanged: (v) => setState(() => _previewThumbnail = v),
            ),
            const SizedBox(height: 12),

            // Category dropdown
            DropdownButtonFormField<String>(
              value: _selectedCategory,
              dropdownColor: AppColors.surface,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                labelText: 'Category *',
                prefixIcon: Icon(Icons.category_outlined,
                    color: AppColors.textTertiary),
              ),
              items: catList
                  .map((c) => DropdownMenuItem(
                        value: c.name,
                        child: Text(c.name,
                            style: const TextStyle(color: Colors.white)),
                      ))
                  .toList(),
              onChanged: (v) =>
                  setState(() => _selectedCategory = v ?? 'Pop'),
            ),
            const SizedBox(height: 12),

            // Language dropdown
            DropdownButtonFormField<String>(
              value: _selectedLanguage,
              dropdownColor: AppColors.surface,
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                labelText: 'Language',
                prefixIcon: Icon(Icons.language_rounded,
                    color: AppColors.textTertiary),
              ),
              items: ['English', 'Hindi', 'Punjabi', 'Tamil', 'Telugu', 'Other']
                  .map((l) => DropdownMenuItem(
                        value: l,
                        child: Text(l,
                            style: const TextStyle(color: Colors.white)),
                      ))
                  .toList(),
              onChanged: (v) =>
                  setState(() => _selectedLanguage = v ?? 'English'),
            ),
            const SizedBox(height: 12),

            _buildTextField(
              controller: _tagsCtrl,
              label: 'Tags (comma separated)',
              icon: Icons.tag_rounded,
              hint: 'happy, energetic, summer',
            ),
            const SizedBox(height: 16),

            // Featured toggle
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: AppColors.surfaceVariant,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: AppColors.glassBorder),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.star_rounded,
                          color: AppColors.warning, size: 22),
                      SizedBox(width: 10),
                      Text('Feature this song',
                          style: TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w500)),
                    ],
                  ),
                  Switch(
                    value: _isFeatured,
                    onChanged: (v) => setState(() => _isFeatured = v),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),

            // Lyrics
            TextFormField(
              controller: _lyricsCtrl,
              style: const TextStyle(color: Colors.white),
              maxLines: 6,
              decoration: const InputDecoration(
                labelText: 'Lyrics (optional)',
                prefixIcon: Icon(Icons.lyrics_outlined,
                    color: AppColors.textTertiary),
                alignLabelWithHint: true,
              ),
            ),
            const SizedBox(height: 32),

            SizedBox(
              width: double.infinity,
              height: 54,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _saveSong,
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : Text(
                        widget.songId != null ? 'Update Song' : 'Add Song',
                        style: const TextStyle(
                            fontSize: 16, fontWeight: FontWeight.w700),
                      ),
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    required IconData icon,
    String? hint,
    String? Function(String?)? validator,
    void Function(String)? onChanged,
  }) {
    return TextFormField(
      controller: controller,
      style: const TextStyle(color: Colors.white),
      validator: validator,
      onChanged: onChanged,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        prefixIcon: Icon(icon, color: AppColors.textTertiary),
      ),
    );
  }
}
