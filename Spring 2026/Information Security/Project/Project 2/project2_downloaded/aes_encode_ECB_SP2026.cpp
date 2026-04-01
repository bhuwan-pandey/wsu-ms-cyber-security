// CEG 47506750 sample code

#include<iostream>
#include<fstream>
#include<sstream>
#include<string>
using namespace std;

#include"cryptopp/cryptlib.h"
#include"cryptopp/hex.h"
#include"cryptopp/filters.h"
#include"cryptopp/des.h"
#include"cryptopp/aes.h"
#include"cryptopp/modes.h"

using namespace CryptoPP;

string aes_encode(string & plain,byte key[])
{
	string cipher;
	try{
		ECB_Mode<AES>::Encryption enc;
		enc.SetKey(key, AES::DEFAULT_KEYLENGTH);
		StringSource(plain, true, new StreamTransformationFilter(enc, new StringSink(cipher)));//add padding by StreamTransformationFilter 
	}
	catch(const CryptoPP::Exception & e)
	{
	}
	return cipher;
}

int main(int argc, char * argv[])
{
	fstream file1;
	fstream file2;
	byte key[AES::DEFAULT_KEYLENGTH];

	if(argc!=4)
	{
		cout<<"usage:aes_encode infile outfile key"<<endl;
		return 0;
	}
	file1.open(argv[1],ios::in);
	file2.open(argv[2],ios::out);
	//reading
	stringstream buffer;  
	buffer << file1.rdbuf();  
	string plain(buffer.str());  
	//cout<<"plain text:"<<plain<<endl;
	//get key
	memset(key,0,AES::DEFAULT_KEYLENGTH);
	for(int i=0;i<AES::DEFAULT_KEYLENGTH;i++)
	{
		if(argv[3][i]!='\0')
		{
			key[i]=(byte)argv[3][i];
		}				
		else
		{
			break;
		}
	}
	//print key
	string encoded;
	encoded.clear();
	StringSource(key, sizeof(key), true, new HexEncoder( new StringSink(encoded))); 
	cout << "key: " << encoded<< endl;
	//encode
	cout << "plain text: " << plain << endl;
	string cipher=aes_encode(plain,key);
	file2<<cipher;
	cout<<"cipher text stored in:"<<argv[2]<<endl;
	
	file1.close();
	file2.close();	
}	
