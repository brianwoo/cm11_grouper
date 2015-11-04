#ifndef _RAR_STRFN_
#define _RAR_STRFN_

const char* NullToEmpty(const char *Str);
#ifndef __BIONIC__
const wchar* NullToEmpty(const wchar *Str);
#endif
char* IntNameToExt(const char *Name);
void ExtToInt(const char *Src,char *Dest);
void IntToExt(const char *Src,char *Dest);
char* strlower(char *Str);
char* strupper(char *Str);
int stricomp(const char *Str1,const char *Str2);
int strnicomp(const char *Str1,const char *Str2,size_t N);
char* RemoveEOL(char *Str);
char* RemoveLF(char *Str);
#ifndef __BIONIC__
wchar* RemoveLF(wchar *Str);
#endif
unsigned char loctolower(unsigned char ch);
unsigned char loctoupper(unsigned char ch);

char* strncpyz(char *dest, const char *src, size_t maxlen);
#ifndef __BIONIC__
wchar* wcsncpyz(wchar *dest, const wchar *src, size_t maxlen);
#endif
char* strncatz(char* dest, const char* src, size_t maxlen);
#ifndef __BIONIC__
wchar* wcsncatz(wchar* dest, const wchar* src, size_t maxlen);
#endif

unsigned char etoupper(unsigned char ch);
#ifndef __BIONIC__
wchar etoupperw(wchar ch);
#endif

bool IsDigit(int ch);
bool IsSpace(int ch);
bool IsAlpha(int ch);


#ifndef SFX_MODULE
uint GetDigits(uint Number);
#endif

bool LowAscii(const char *Str);
#ifndef __BIONIC__
bool LowAscii(const wchar *Str);
#endif


int stricompc(const char *Str1,const char *Str2);
#ifndef SFX_MODULE
#ifndef __BIONIC__
int wcsicompc(const wchar *Str1,const wchar *Str2);
#endif
#endif

void itoa(int64 n,char *Str);
int64 atoil(const char *Str);
#ifndef __BIONIC__
void itoa(int64 n,wchar *Str);
int64 atoil(const wchar *Str);
const wchar* GetWide(const char *Src);
const wchar* GetWide(const char *Src,const wchar *SrcW);
const wchar* GetCmdParam(const wchar *CmdLine,wchar *Param,size_t MaxSize);
#endif

#endif
