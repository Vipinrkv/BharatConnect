import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Compass, Heart, MessageSquare, Share2, Award, ArrowUpRight, 
  MapPin, Send, MessageCircleCode, CheckCircle2, Sparkles 
} from 'lucide-react';
import { NearbyPost, UserProfile } from '../types';

interface NearbyFeedProps {
  initialPosts: NearbyPost[];
  currentUser: UserProfile;
  isDarkMode: boolean;
}

export default function NearbyFeedView({ initialPosts, currentUser, isDarkMode }: NearbyFeedProps) {
  const [posts, setPosts] = useState<NearbyPost[]>(initialPosts);
  const [newPostText, setNewPostText] = useState('');
  const [selectedTag, setSelectedTag] = useState<string | null>(null);

  const handleLike = (id: string) => {
    setPosts((prev) =>
      prev.map((p) => {
        if (p.id === id) {
          const hasLiked = !p.hasLiked;
          return {
            ...p,
            hasLiked,
            likes: hasLiked ? p.likes + 1 : p.likes - 1,
          };
        }
        return p;
      })
    );
  };

  const handleCreatePost = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPostText.trim()) return;

    const newPost: NearbyPost = {
      id: `post_${Date.now()}`,
      user: currentUser,
      content: newPostText,
      timestamp: 'Just Now',
      likes: 0,
      comments: 0,
      hasLiked: false,
      distance: '10m away',
      tag: 'General Bulletin'
    };

    setPosts([newPost, ...posts]);
    setNewPostText('');
  };

  const uniqueTags = Array.from(new Set(posts.map((p) => p.tag).filter(Boolean))) as string[];

  const filteredPosts = selectedTag 
    ? posts.filter((p) => p.tag === selectedTag)
    : posts;

  return (
    <div id="nearby_feed_container" className="flex flex-col h-full w-full">
      
      {/* Dynamic Animated Radar sweep matching Telegram */}
      <div className={`p-5 flex flex-col items-center justify-center border-b shrink-0 relative overflow-hidden ${
        isDarkMode ? 'bg-elegant-card/55 border-elegant-border/15' : 'bg-[#f5f2eb]/70 border-[#dfd5c6]/40'
      }`}>
        {/* Animated Radar Background */}
        <div className={`absolute w-36 h-36 rounded-full border flex items-center justify-center animate-ping duration-[3.5s] ${
          isDarkMode ? 'border-elegant-gold/15' : 'border-[#c5a880]/20'
        }`} />
        <div className={`absolute w-24 h-24 rounded-full border flex items-center justify-center animate-pulse ${
          isDarkMode ? 'border-elegant-gold/25' : 'border-[#c5a880]/30'
        }`} />
        
        <div className="relative z-10 text-center flex flex-col items-center">
          <div className={`w-14 h-14 rounded-full flex items-center justify-center border text-elegant-gold mb-2 relative ${
            isDarkMode ? 'bg-elegant-card border-elegant-gold/30' : 'bg-[#f5f2eb] border-[#c5a880]/40'
          }`}>
            <Compass className="w-7 h-7 animate-spin duration-1000" style={{ animationDuration: '8s' }} />
            <span className={`absolute top-0 right-0 w-3 h-3 rounded-full border ${
              isDarkMode ? 'bg-elegant-gold border-elegant-bg' : 'bg-elegant-gold-dark border-white'
            }`} />
          </div>
          
          <h4 className={`text-xs font-bold font-mono tracking-wider uppercase ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>
            Geographic Proximity Radar
          </h4>
          <p className="text-[10px] text-slate-400 font-mono mt-1">
            Tracking active crisis coordinators & supply caches within <span className={`font-bold ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`}>1.5 km</span>
          </p>
        </div>
      </div>

      {/* Posting area */}
      <div className={`p-4 border-b ${isDarkMode ? 'border-elegant-border/10 bg-elegant-bg/40' : 'border-[#dfd5c6]/30 bg-white'}`}>
        <form onSubmit={handleCreatePost} className="flex gap-3">
          {/* Avatar */}
          <div className={`w-8 h-8 rounded-full ${currentUser.avatarBg} flex items-center justify-center font-bold text-xs text-white shrink-0`}>
            {currentUser.avatar}
          </div>

          <div className="flex-1 flex flex-col gap-2">
            <textarea
              id="input_new_post"
              rows={2}
              value={newPostText}
              onChange={(e) => setNewPostText(e.target.value)}
              placeholder="What is happening in your immediate block?"
              className={`w-full p-2.5 rounded-xl text-xs border outline-none resize-none duration-150 ${
                isDarkMode 
                  ? 'bg-elegant-card border-elegant-border/20 text-slate-200 placeholder-slate-600 focus:ring-1 focus:ring-elegant-gold/45' 
                  : 'bg-[#fbfaf6] border-[#dfd5c6] text-[#3c3730] placeholder-slate-400 focus:ring-1 focus:ring-[#c5a880]/45'
              }`}
            />
            
            <div className="flex justify-between items-center">
              <span className={`text-[9px] font-mono ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
                📌 Tags as "General Bulletin" • Under 280 chars
              </span>
              <button
                id="btn_post_nearby"
                type="submit"
                className="bg-gradient-to-r from-elegant-gold-dark to-elegant-gold shadow-md text-elegant-bg font-extrabold text-[11px] px-3.5 py-1.5 rounded-lg flex items-center gap-1.5 hover:brightness-110 cursor-pointer active:scale-95 transition-all"
              >
                Post Live Alert
                <Send className="w-3 h-3 stroke-[2.5]" />
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* Filter Category Tabs for Nearby Posts */}
      <div className="p-3 pb-1 flex gap-1.5 overflow-x-auto scrollbar-none shrink-0">
        <button
          id="btn_filter_tag_all"
          onClick={() => setSelectedTag(null)}
          className={`px-3.5 py-1.5 rounded-full text-[10px] font-mono border transition-all cursor-pointer ${
            selectedTag === null
              ? 'bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg font-bold border-transparent shadow-sm'
              : isDarkMode
              ? 'bg-elegant-card border-elegant-border/15 text-slate-400 hover:text-[#f3f4f6]'
              : 'bg-[#f5f2eb] border-[#dfd5c6]/60 text-[#5c5346] hover:text-[#1c1a17]'
          }`}
        >
          All Feeds
        </button>
        {uniqueTags.map((tag) => (
          <button
            key={tag}
            id={`btn_filter_tag_${tag.replace(/\s+/g, '_')}`}
            onClick={() => setSelectedTag(tag)}
            className={`px-3.5 py-1.5 rounded-full text-[10px] font-mono border shrink-0 transition-all cursor-pointer ${
              selectedTag === tag
                ? 'bg-gradient-to-r from-elegant-gold-dark to-elegant-gold text-elegant-bg font-bold border-transparent shadow-sm'
                : isDarkMode
                ? 'bg-elegant-card border-elegant-border/15 text-slate-400 hover:text-[#f3f4f6]'
                : 'bg-[#f5f2eb] border-[#dfd5c6]/60 text-[#5c5346] hover:text-[#1c1a17]'
            }`}
          >
            {tag}
          </button>
        ))}
      </div>

      {/* Feed listing */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4 scrollbar-thin">
        <AnimatePresence>
          {filteredPosts.length > 0 ? (
            filteredPosts.map((post) => (
              <motion.div
                key={post.id}
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                className={`p-4 rounded-2xl border transition-all ${
                  isDarkMode 
                    ? 'bg-elegant-card border-elegant-border/10 hover:border-elegant-gold/20' 
                    : 'bg-white border-[#dfd5c6]/45 shadow-sm hover:shadow-md'
                }`}
              >
                {/* Header info */}
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    {/* User profile details */}
                    <div className={`w-8 h-8 rounded-full ${post.user.avatarBg} flex items-center justify-center font-bold text-xs text-white relative`}>
                      {post.user.avatar}
                    </div>

                    <div className="flex flex-col">
                      <div className="flex items-center gap-1">
                        <span className={`text-xs font-semibold ${isDarkMode ? 'text-slate-200' : 'text-[#3c3730]'}`}>
                          {post.user.name}
                        </span>
                        {post.user.role === 'verified_responder' && (
                          <CheckCircle2 className={`w-3.5 h-3.5 ${isDarkMode ? 'text-elegant-gold' : 'text-elegant-gold-dark'}`} />
                        )}
                        {post.user.role === 'admin' && (
                          <div className={`text-[8px] px-1 py-0.5 rounded uppercase font-mono tracking-tight font-extrabold ${
                            isDarkMode ? 'bg-elegant-gold/15 text-elegant-gold' : 'bg-[#dfd5c6] text-elegant-gold-dark'
                          }`}>Admin</div>
                        )}
                      </div>
                      <span className="text-[9px] text-slate-400 font-mono leading-none">{post.user.username}</span>
                    </div>
                  </div>

                  {/* Distance Indicator pill */}
                  <div className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-mono border ${
                    isDarkMode ? 'bg-elegant-bg text-elegant-gold border-elegant-border/15' : 'bg-[#dfd5c6]/20 text-elegant-gold-dark border-[#dfd5c6]/50'
                  }`}>
                    <MapPin className="w-3 h-3" />
                    <span>{post.distance}</span>
                  </div>
                </div>

                {/* Content block */}
                <p className={`text-xs leading-relaxed mb-3 ${isDarkMode ? 'text-slate-300' : 'text-slate-700'}`}>
                  {post.content}
                </p>

                {/* Tag item details if applicable */}
                {post.tag && (
                  <span className={`inline-block text-[9.5px] font-mono px-2 py-0.5 rounded-md mb-3 ${
                    isDarkMode ? 'bg-elegant-bg text-slate-400 border border-elegant-border/10 animate-pulse' : 'bg-[#f5f2eb] text-[#5c5346] border border-[#dfd5c6]/40'
                  }`}>
                    🏷️ {post.tag}
                  </span>
                )}

                {/* Footer interactives */}
                <div className="flex items-center gap-6 pt-2 border-t border-dashed border-slate-800/10 dark:border-white/5">
                  <button
                    id={`btn_like_post_${post.id}`}
                    onClick={() => handleLike(post.id)}
                    className={`flex items-center gap-1.5 text-xs font-mono transition-all duration-150 cursor-pointer ${
                      post.hasLiked 
                        ? 'text-rose-500 font-bold scale-105' 
                        : 'text-slate-400 hover:text-rose-450'
                    }`}
                  >
                    <Heart className={`w-4 h-4 ${post.hasLiked ? 'fill-current text-rose-500' : ''}`} />
                    <span>{post.likes}</span>
                  </button>

                  <button
                    id={`btn_comment_post_${post.id}`}
                    onClick={() => {
                      // Simulates quick action response
                      const newComment = prompt(`Reply to ${post.user.name}'s broadcast:`);
                      if (newComment) {
                        alert(`Your response has been transmitted to ${post.user.name}'s secure inbox.`);
                      }
                    }}
                    className={`flex items-center gap-1.5 text-xs font-mono transition-colors cursor-pointer text-slate-400 ${
                      isDarkMode ? 'hover:text-elegant-gold' : 'hover:text-[#9e835e]'
                    }`}
                  >
                    <MessageSquare className="w-4 h-4" />
                    <span>{post.comments} Responses</span>
                  </button>

                  <button
                    id={`btn_share_post_${post.id}`}
                    onClick={() => alert('Encrypted broadcast share-link copied to clip.')}
                    className={`ml-auto text-xs font-mono transition-colors cursor-pointer text-slate-400 ${
                      isDarkMode ? 'hover:text-elegant-gold' : 'hover:text-[#9e835e]'
                    }`}
                    title="Transmit Broadcast Link"
                  >
                    <Share2 className="w-4 h-4" />
                  </button>
                </div>
              </motion.div>
            ))
          ) : (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Compass className="w-8 h-8 text-slate-600 mb-2" />
              <p className="text-xs text-slate-400">No active posts found targeting this channel.</p>
            </div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
