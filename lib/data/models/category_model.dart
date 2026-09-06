// lib/data/models/category_model.dart
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';

class CategoryModel {
  final String id;
  final String name;
  final String iconUrl;
  final String color;
  final String description;
  final int order;
  final int songCount;

  const CategoryModel({
    required this.id,
    required this.name,
    this.iconUrl = '',
    this.color = '#6C63FF',
    this.description = '',
    this.order = 0,
    this.songCount = 0,
  });

  factory CategoryModel.fromFirestore(DocumentSnapshot doc) {
    final data = doc.data() as Map<String, dynamic>;
    return CategoryModel(
      id: doc.id,
      name: data['name'] ?? '',
      iconUrl: data['iconUrl'] ?? '',
      color: data['color'] ?? '#6C63FF',
      description: data['description'] ?? '',
      order: data['order'] ?? 0,
      songCount: data['songCount'] ?? 0,
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'name': name,
      'iconUrl': iconUrl,
      'color': color,
      'description': description,
      'order': order,
      'songCount': songCount,
    };
  }

  Color get displayColor {
    try {
      final hex = color.replaceFirst('#', '');
      return Color(int.parse('FF$hex', radix: 16));
    } catch (_) {
      return const Color(0xFF6C63FF);
    }
  }

  static final List<CategoryModel> defaults = [
    const CategoryModel(id: 'pop', name: 'Pop', color: '#6C63FF', order: 0),
    const CategoryModel(id: 'rock', name: 'Rock', color: '#FF6B9D', order: 1),
    const CategoryModel(id: 'hiphop', name: 'Hip-Hop', color: '#00D9FF', order: 2),
    const CategoryModel(id: 'classical', name: 'Classical', color: '#FFD740', order: 3),
    const CategoryModel(id: 'edm', name: 'EDM', color: '#00E676', order: 4),
    const CategoryModel(id: 'jazz', name: 'Jazz', color: '#FF6E40', order: 5),
    const CategoryModel(id: 'rb', name: 'R&B', color: '#CE93D8', order: 6),
    const CategoryModel(id: 'bollywood', name: 'Bollywood', color: '#F48FB1', order: 7),
    const CategoryModel(id: 'lofi', name: 'Lofi', color: '#80DEEA', order: 8),
    const CategoryModel(id: 'devotional', name: 'Devotional', color: '#A5D6A7', order: 9),
  ];
}
