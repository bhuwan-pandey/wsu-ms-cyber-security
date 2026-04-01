//Provided by Meilin Liu

#include<iostream>
#include<fstream>
#include<sstream>
#include<string>
using namespace std;

#include"cryptopp/cryptlib.h"
#include"cryptopp/hex.h"
#include"cryptopp/filters.h"
#include"cryptopp/des.h"
#include"cryptopp/modes.h"

using namespace CryptoPP;
string des_decode(string & cipher,byte key[], byte iv[])
{
	string plain;
	//decryption
	try
	{
		CBC_Mode< DES >::Decryption dec;
		dec.SetKeyWithIV(key, DES::DEFAULT_KEYLENGTH,iv);
		StringSource s(cipher, true, new StreamTransformationFilter(dec, new StringSink(plain)));  
		cout << "recovered text: " << plain<< endl;
	}
	catch(const CryptoPP::Exception& e)
	{
		cerr << e.what() << endl;
		exit(1);
	}
	return plain;	
}
int main(int argc,char * argv[])
{
	ifstream file1;
	ofstream file2;
    
	byte key[DES::DEFAULT_KEYLENGTH]={'1','2','3','4','a','b','c','d'}; //key is hardcoded;
	byte iv[DES::BLOCKSIZE] = {0}; // the same iv used in encryption
	
	if(argc!=3)
	{
		cout<<"usage:des_decode infile outfile" <<endl;
	}
	
	cout << " The input file name is " << argv [1] << endl;
	cout << " The output file name is " << argv [2] << endl;
 
	
	file1.open(argv[1]);
	file2.open(argv[2]);
	//reading 
	stringstream buffer;  
	buffer << file1.rdbuf();  
	string cipher(buffer.str());  

	
	//print key
	string encoded;
	encoded.clear();
	StringSource(key, sizeof(key), true, new HexEncoder( new StringSink(encoded))); 
	cout << "key: " << encoded << endl;
	
	//decryption; 
	string plain=des_decode(cipher,key,iv);
	
	file2<<plain;
	
	cout<<"plain text stored in:"<<argv[2]<<endl;
	
	file1.close();
	file2.close();	
	return 0;
	
}
