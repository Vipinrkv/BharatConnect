import os
from PIL import Image, ImageDraw

src_path = r'C:\Users\Vipin\.gemini\antigravity\brain\aea96e34-65db-4f2d-a451-d0c19e1b557b\bharatconnect_logo_1786263357575.jpg'
img = Image.open(src_path).convert("RGBA")

# Mipmap sizes
sizes = {
    'mipmap-mdpi': (48, 48),
    'mipmap-hdpi': (72, 72),
    'mipmap-xhdpi': (96, 96),
    'mipmap-xxhdpi': (144, 144),
    'mipmap-xxxhdpi': (192, 192)
}

res_dir = r'c:\Users\Vipin\OneDrive\Desktop\WebAplications\BharatConnect\android_app\app\src\main\res'

def make_round(im):
    mask = Image.new('L', im.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, im.size[0], im.size[1]), fill=255)
    result = im.copy()
    result.putalpha(mask)
    return result

for folder, size in sizes.items():
    target_dir = os.path.join(res_dir, folder)
    os.makedirs(target_dir, exist_ok=True)
    
    resized = img.resize(size, Image.Resampling.LANCZOS)
    resized.save(os.path.join(target_dir, 'ic_launcher.png'))
    
    round_img = make_round(resized)
    round_img.save(os.path.join(target_dir, 'ic_launcher_round.png'))
    print(f"Updated {folder} ({size[0]}x{size[1]})")

# Also update web asset logo
www_assets_dir = r'c:\Users\Vipin\OneDrive\Desktop\WebAplications\BharatConnect\android_app\app\src\main\assets\www'
web_logo = img.resize((512, 512), Image.Resampling.LANCZOS)
web_logo.save(os.path.join(www_assets_dir, 'logo.png'))
print("Updated web asset logo.png (512x512)")
