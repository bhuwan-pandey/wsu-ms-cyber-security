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

string des_encode(string & plain,byte key[])
{
	string cipher;
	try
	{
		//cout << "plain text: " << plain << endl;
		ECB_Mode<DES>::Encryption enc;
		enc.SetKey(key, DES::DEFAULT_KEYLENGTH);
		StringSource(plain, true, new StreamTransformationFilter(enc, new StringSink(cipher)));//add padding by StreamTransformationFilter 
	}
	catch(const CryptoPP::Exception& e)
	{
		cerr << e.what() << endl;
		exit(1);
	}
	return cipher;
}

int main(int argc, char * argv[])
{
	ifstream file1;
	ofstream file2;
	byte key[DES::DEFAULT_KEYLENGTH]={'1','2','3','4','a','b','c','d'}; //key is hardcoded
	
	if(argc!=3)
	{
		cout<<"usage:des_encode infile outfile" << endl;
	}
	
	cout << " The input file name is " << argv [1] << endl;
	cout << " The output file name is " << argv [2] << endl;
	
	
	file1.open(argv[1]);
	file2.open(argv[2]);
	
	//reading
	stringstream buffer;  
	buffer << file1.rdbuf();  
	
	string plain(buffer.str());  

    
	//print key
	string encoded;
	encoded.clear();
	StringSource(key, sizeof(key), true, new HexEncoder( new StringSink(encoded))); 
	
	cout << "key: " << encoded << endl;
	
	//encrypt
	
	string cipher=des_encode(plain,key);
	file2<<cipher;
	
	cout<<"cipher text stored in:"<<argv[2]<<endl;
	
	file1.close();
	file2.close();	
	return 0; 
}	
