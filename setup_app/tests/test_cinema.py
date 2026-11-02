import unittest
from setup_app.cinema import validate,call
from setup_app.core import SetupError
class CinemaTests(unittest.TestCase):
    def test_world_bindings_round_trip_without_changing_other_fields(self):
        value={'schema':1,'worldsEnabled':True,'worlds':[{'id':'hogwarts','wallpaper':'p-00000000-0000-0000-0000-000000000000','idleArt':'a'*64,'covers':['b'*64]}]}
        self.assertEqual(validate(value),value)
        self.assertNotIn('wallpaper_playlist_v1',value)
    def test_worlds_reject_shared_backgrounds_executable_fields_and_paths(self):
        world={'id':'hogwarts','wallpaper':'p-00000000-0000-0000-0000-000000000000'}
        for worlds in ([world,dict(world,id='hobbit')],[dict(world,script='run')],[dict(world,id='unknown')],[dict(world,hopArt='../file')],[dict(world,covers=['https://example.org'])]):
            with self.assertRaises(SetupError):validate({'schema':1,'worlds':worlds})
    def test_empty_setup_keeps_features_optional(self):self.assertEqual(validate({'schema':1}),{'schema':1})
    def test_reject_executable_fields(self):
        for draft in ({'schema':1,'script':'run'},{'schema':True},{'schema':1,'logos':'on'},{'schema':1,'scene':'https://example.org'}):
            with self.assertRaises(SetupError):validate(draft)
    def test_titles_have_bounded_local_assets_and_safe_links(self):
        title={'id':'film','name':'Film','package':'org.example.app','moods':['Comfort'],'url':'https://example.org/title/1'}
        self.assertTrue(validate({'schema':1,'moods':['Comfort'],'titles':[title]}))
        for link in ('http://example.org','intent://open','https://user:secret@example.org','https://example.org:444'):
            with self.assertRaises(SetupError):validate({'schema':1,'moods':['Comfort'],'titles':[dict(title,url=link)]})
        with self.assertRaises(SetupError):validate({'schema':1,'sceneArt':'../image.png'})
    def test_reject_duplicate_titles_unknown_moods(self):
        t={'id':'film','name':'Film','package':'org.example.app'}
        with self.assertRaises(SetupError):validate({'schema':1,'titles':[t,t]})
        with self.assertRaises(SetupError):validate({'schema':1,'titles':[dict(t,moods=['Unknown'])]})
    def test_no_connection_no_mutation(self):
        class Session:mode='preview';connection=None
        with self.assertRaises(SetupError):call(Session(),'cinema_stage',{})
    def test_only_a_unique_compatible_identity_is_targeted(self):
        from types import SimpleNamespace
        commands=[]
        def shell(command,**kwargs):
            commands.append(command)
            return 'Bundle[{json={"schema":1,"reviewRequired":true}}]'
        connection=SimpleNamespace(paired=False,device=SimpleNamespace(shell=shell),read=lambda key:'package:com.dvopenlabs.cytv.usabilitytest\n')
        session=SimpleNamespace(mode='device',connection=connection)
        call(session,'cinema_read',{})
        self.assertIn('content://com.dvopenlabs.cytv.usabilitytest.theme',commands[0])
        with self.assertRaises(SetupError):call(session,'cinema_stage',{'schema':1,'package':'com.dvopenlabs.freedomlauncher','profile':'local_test','revision':'abc','draft':{'schema':1}})
        self.assertEqual(len(commands),1)
        connection.read=lambda key:'package:com.dvopenlabs.cytv.usabilitytest\npackage:com.dvopenlabs.freedomlauncher\n'
        with self.assertRaises(SetupError):call(session,'cinema_read',{})
        self.assertEqual(len(commands),1)
    def test_draft_staging_never_calls_an_apply_endpoint(self):
        from types import SimpleNamespace
        commands=[]
        def shell(command,**kwargs):
            commands.append(command)
            return 'Bundle[{json={"schema":1,"reviewRequired":true}}]'
        session=SimpleNamespace(mode='device',connection=SimpleNamespace(paired=False,device=SimpleNamespace(shell=shell),read=lambda key:'package:com.dvopenlabs.freedomlauncher\n'))
        result=call(session,'cinema_stage',{'schema':1,'package':'com.dvopenlabs.freedomlauncher','profile':'local_test','revision':'abc','draft':{'schema':1,'iconStyle':'gold'}})
        self.assertTrue(result['reviewRequired']);self.assertEqual(len(commands),1)
        self.assertIn('--method cinema_stage',commands[0]);self.assertNotIn('--method product_layout_apply',commands[0])
    def test_default_profile_can_stage_but_empty_revision_and_bool_schema_cannot(self):
        from types import SimpleNamespace
        commands=[]
        def shell(command,**kwargs):
            commands.append(command)
            return 'Bundle[{json={"schema":1,"reviewRequired":true}}]'
        session=SimpleNamespace(mode='device',connection=SimpleNamespace(paired=False,device=SimpleNamespace(shell=shell),read=lambda key:'package:com.dvopenlabs.freedomlauncher\n'))
        request={'schema':1,'package':'com.dvopenlabs.freedomlauncher','profile':'','revision':'abc','draft':{'schema':1}}
        self.assertTrue(call(session,'cinema_stage',request)['reviewRequired'])
        for change in ({'revision':''},{'schema':True}):
            with self.assertRaises(SetupError):call(session,'cinema_stage',dict(request,**change))
        self.assertEqual(len(commands),1)
