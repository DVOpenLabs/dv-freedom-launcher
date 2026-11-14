import importlib.util,json,tempfile,unittest,zipfile,struct,zlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('world_pack',ROOT/'tools/world_pack.py');pack=importlib.util.module_from_spec(spec);spec.loader.exec_module(pack)
class WorldPackTest(unittest.TestCase):
 def fixture(self,p):
  (p/'manifest.json').write_text(json.dumps(dict(schema=1,id='quiet-world',name='Quiet world',version=1)))
  (p/'config.json').write_text('{"wallpaper":true}')
  (p/'cinema.json').write_text(json.dumps(dict(schema=1,worldsEnabled=True,worlds=[dict(id='hobbit',wallpaper=pack.SELF)])))
  def chunk(kind,raw):return struct.pack('>I',len(raw))+kind+raw+struct.pack('>I',zlib.crc32(kind+raw)&0xffffffff)
  png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',2,2,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b'\0'+b'\0'*6+b'\0'+b'\0'*6))+chunk(b'IEND',b'')
  (p/'CREDITS.txt').write_text('Original test fixture; no redistributed media.');(p/'wallpaper.jpg').write_bytes(png)
 def test_invalid_fallback_and_visual_values_are_refused(self):
  with tempfile.TemporaryDirectory()as t:
   p=Path(t);self.fixture(p);(p/'config.json').write_text('{"wallpaper_dim":true}')
   with self.assertRaises(ValueError):pack.build(p,p/'out.zip')
   (p/'config.json').write_text('{}');(p/'wallpaper.jpg').write_bytes(b'not an image')
   with self.assertRaises(ValueError):pack.build(p,p/'out.zip')
 def test_portable_world_never_includes_unrelated_private_files(self):
  with tempfile.TemporaryDirectory()as t:
   p=Path(t);self.fixture(p);(p/'private-token.txt').write_text('never exported');out=p/'out.zip';r=pack.build(p,out)
   with zipfile.ZipFile(out)as z:self.assertNotIn('private-token.txt',z.namelist());self.assertIn('cinema.json',z.namelist())
   with self.assertRaises(ValueError):pack.build(p,out)
 def test_private_movie_links_and_cross_device_ids_are_refused(self):
  with tempfile.TemporaryDirectory()as t:
   p=Path(t);self.fixture(p);f=p/'cinema.json';d=json.loads(f.read_text());d['worlds'][0]['wallpaper']='p-11111111-1111-1111-1111-111111111111';f.write_text(json.dumps(d))
   with self.assertRaises(ValueError):pack.build(p,p/'out.zip')
 def test_artwork_must_match_its_content_hash(self):
  with tempfile.TemporaryDirectory()as t:
   p=Path(t);self.fixture(p);f=p/'cinema.json';d=json.loads(f.read_text());d['worlds'][0]['covers']=['0'*64];f.write_text(json.dumps(d));(p/'art').mkdir();(p/'art'/('0'*64+'.png')).write_bytes(b'not png')
   with self.assertRaises(ValueError):pack.build(p,p/'out.zip')
